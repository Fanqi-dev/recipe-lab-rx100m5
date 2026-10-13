package com.voxivoid.recipelab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.util.*;

class SafeSettingsTest {
    @TempDir File dir;
    static class Camera implements SafeSettings.Store {
        final Map<Integer,byte[]> values = new LinkedHashMap<Integer,byte[]>();
        int failId = -1, attrValue = 0, writes = 0; boolean failed, corrupt, crash;
        Camera() { values.put(Params.ID_CON,new byte[]{0}); values.put(Params.ID_SAT,new byte[]{0}); }
        public byte[] read(int id) throws Exception { byte[] v=values.get(id); if(v==null) throw new IOException("missing"); return v.clone(); }
        public int attr(int id) { return attrValue; }
        public void write(int id,byte[] v) throws Exception {
            writes++;
            if(id==failId && !failed) { failed=true; if(crash) throw new AssertionError("power loss"); throw new IOException("write failed"); }
            values.put(id,v.clone());
            if(corrupt && !failed) { failed=true; values.put(id,new byte[]{99}); }
        }
        public void sync() {}
    }
    List<Params.Write> writes() { return Arrays.asList(new Params.Write(Params.ID_CON,2),new Params.Write(Params.ID_SAT,-1)); }
    File journal() { return new File(dir,"settings.txn"); }
    @Test void successVerifiedAndJournalCleared() throws Exception {
        Camera c=new Camera(); new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes());
        assertEquals(2,c.read(Params.ID_CON)[0]); assertEquals(-1,c.read(Params.ID_SAT)[0]); assertFalse(journal().exists());
    }
    @Test void failedSecondWriteRestoresFirstAndFailedSlot() {
        Camera c=new Camera(); c.failId=Params.ID_SAT;
        SafeSettings.Failure e=assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes()));
        assertFalse(e.blocked); assertEquals(0,c.values.get(Params.ID_CON)[0]); assertEquals(0,c.values.get(Params.ID_SAT)[0]); assertFalse(journal().exists());
    }
    @Test void partialNativeWriteBeforeExceptionIsAlsoRestored() {
        Camera c=new Camera() { public void write(int id,byte[] v) throws Exception { values.put(id,v.clone()); super.write(id,v); } }; c.failId=Params.ID_SAT;
        assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes()));
        assertEquals(0,c.values.get(Params.ID_CON)[0]); assertEquals(0,c.values.get(Params.ID_SAT)[0]);
    }
    @Test void wrongModelNeverWrites() {
        Camera c=new Camera(); assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).apply("DSC-RX100M5A",writes())); assertEquals(0,c.writes);
    }
    @Test void unknownAttributesNeverWrite() {
        Camera c=new Camera(); c.attrValue=-1; assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes())); assertEquals(0,c.writes);
    }
    @Test void readonlyNeverWrites() {
        Camera c=new Camera(); c.attrValue=1; assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes())); assertEquals(0,c.writes);
    }
    @Test void imageQualitySlotsAreNeverWritten() {
        Camera c=new Camera();
        List<Params.Write> request=Arrays.asList(new Params.Write(Params.ID_CON,2),new Params.Write(Params.ID_QJPG,1));
        assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,request)); assertEquals(0,c.writes);
    }
    @Test void missingSlotNeverWritesAnySlot() {
        Camera c=new Camera(); c.values.remove(Params.ID_SAT); assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes())); assertEquals(0,c.writes);
    }
    @Test void wrongSizeNeverWrites() {
        Camera c=new Camera(); c.values.put(Params.ID_SAT,new byte[]{0,0}); assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes())); assertEquals(0,c.writes);
    }
    @Test void readbackMismatchRollsBack() {
        Camera c=new Camera(); c.corrupt=true; assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes())); assertEquals(0,c.values.get(Params.ID_CON)[0]);
    }
    @Test void interruptionLeavesJournalAndRestartRestores() throws Exception {
        Camera c=new Camera(); c.failId=Params.ID_SAT; c.crash=true;
        assertThrows(AssertionError.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes()));
        assertTrue(journal().exists()); assertEquals(2,c.values.get(Params.ID_CON)[0]);
        new SafeSettings(c,journal()).recover(SafeSettings.MODEL);
        assertEquals(0,c.values.get(Params.ID_CON)[0]); assertFalse(journal().exists());
    }
    @Test void failedRecoveryRetainsJournalAndBlocks() throws Exception {
        Camera c=new Camera(); c.failId=Params.ID_SAT; c.crash=true;
        assertThrows(AssertionError.class,()->new SafeSettings(c,journal()).apply(SafeSettings.MODEL,writes()));
        c.attrValue=-1;
        assertTrue(assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).recover(SafeSettings.MODEL)).blocked);
        assertTrue(journal().exists());
    }
    @Test void corruptJournalNeverWrites() throws Exception {
        SafeSettings.writeFile(journal(),new byte[]{1,2,3},true); Camera c=new Camera();
        assertThrows(SafeSettings.Failure.class,()->new SafeSettings(c,journal()).recover(SafeSettings.MODEL)); assertEquals(0,c.writes);
    }
}
