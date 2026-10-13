package com.voxivoid.recipelab;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;

class SafeRecipeFilesTest {
    @TempDir File dir;
    byte[] old={1,2,3}, fresh={4,5,6};
    File file(String n){return new File(dir,n);}
    void original() throws Exception { Files.write(file("TEST.YML").toPath(),old); }
    @Test void overwriteVerified() throws Exception {
        original(); SafeRecipeFiles.save(dir,"TEST.YML","TEST.YML",fresh);
        assertArrayEquals(fresh,Files.readAllBytes(file("TEST.YML").toPath())); assertFalse(file(SafeRecipeFiles.JOURNAL).exists());
    }
    @Test void renameFailurePreservesOld() throws Exception {
        original(); SafeRecipeFiles.Ops ops=(a,b)->a.getName().equals(SafeRecipeFiles.TEMP)?false:a.renameTo(b);
        assertThrows(IOException.class,()->SafeRecipeFiles.save(dir,"TEST.YML","TEST.YML",fresh,ops));
        assertArrayEquals(old,Files.readAllBytes(file("TEST.YML").toPath()));
    }
    @Test void interruptionAfterBackupRestoresOnNextLoad() throws Exception {
        original(); SafeRecipeFiles.Ops ops=(a,b)->{if(a.getName().equals(SafeRecipeFiles.TEMP))throw new AssertionError("power loss"); return a.renameTo(b);};
        assertThrows(AssertionError.class,()->SafeRecipeFiles.save(dir,"TEST.YML","TEST.YML",fresh,ops));
        assertTrue(file(SafeRecipeFiles.BACKUP).exists()); SafeRecipeFiles.recover(dir);
        assertArrayEquals(old,Files.readAllBytes(file("TEST.YML").toPath()));
    }
    @Test void interruptionAfterInstallKeepsVerifiedNewOnRestart() throws Exception {
        original(); SafeRecipeFiles.Ops ops=(a,b)->{boolean ok=a.renameTo(b); if(a.getName().equals(SafeRecipeFiles.TEMP))throw new AssertionError("power loss"); return ok;};
        assertThrows(AssertionError.class,()->SafeRecipeFiles.save(dir,"TEST.YML","TEST.YML",fresh,ops));
        SafeRecipeFiles.recover(dir); assertArrayEquals(fresh,Files.readAllBytes(file("TEST.YML").toPath())); assertFalse(file(SafeRecipeFiles.BACKUP).exists());
    }
    @Test void corruptJournalPreservesOriginal() throws Exception {
        original(); Files.write(file(SafeRecipeFiles.JOURNAL).toPath(),new byte[]{1});
        assertThrows(IOException.class,()->SafeRecipeFiles.save(dir,"TEST.YML","TEST.YML",fresh)); assertArrayEquals(old,Files.readAllBytes(file("TEST.YML").toPath()));
    }
    @Test void orphanBackupIsNotDeleted() throws Exception {
        Files.write(file(SafeRecipeFiles.BACKUP).toPath(),old);
        assertThrows(IOException.class,()->SafeRecipeFiles.recover(dir)); assertArrayEquals(old,Files.readAllBytes(file(SafeRecipeFiles.BACKUP).toPath()));
    }
    @Test void unrelatedDestinationCannotBeOverwritten() throws Exception {
        original(); Files.write(file("OTHER.YML").toPath(),fresh);
        assertThrows(IOException.class,()->SafeRecipeFiles.save(dir,"OTHER.YML","TEST.YML",fresh)); assertArrayEquals(old,Files.readAllBytes(file("TEST.YML").toPath()));
    }
}
