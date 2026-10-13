package com.voxivoid.recipelab;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** RX100M5-only, read-before-write with a durable undo journal. Never bypass protection. */
final class SafeSettings {
    static final String MODEL = "DSC-RX100M5";
    private static final int MAGIC = 0x52583531;
    interface Store {
        byte[] read(int id) throws Exception;
        int attr(int id) throws Exception;
        void write(int id, byte[] value) throws Exception;
        void sync() throws Exception;
    }
    static final class Failure extends Exception {
        final boolean blocked;
        Failure(String message, boolean blocked) { super(message); this.blocked = blocked; }
    }
    private final Store store;
    private final File journal;
    SafeSettings(Store store, File journal) { this.store = store; this.journal = journal; }
    static void requireModel(String model) throws IOException {
        if (!MODEL.equals(model)) throw new IOException("RX100M5 only; detected: " + model);
    }
    private byte[] read(int id) throws Exception {
        if (!Params.allSlots().contains(id)) throw new IOException("Unknown slot " + Integer.toHexString(id));
        byte[] b = store.read(id);
        if (b == null || b.length != 1) throw new IOException("Unsupported slot " + Integer.toHexString(id));
        return b;
    }
    void recover(String model) throws Failure {
        try {
            requireModel(model);
            if (!journal.exists()) return;
            LinkedHashMap<Integer, byte[]> old = decode(readFile(journal, 2048));
            restore(old);
            if (!journal.delete()) throw new IOException("Cannot clear recovery journal");
        } catch (Exception e) { throw new Failure("Recovery pending: " + e.getMessage(), true); }
    }
    void apply(String model, List<Params.Write> writes) throws Failure {
        LinkedHashMap<Integer, byte[]> old = new LinkedHashMap<Integer, byte[]>();
        boolean started = false;
        try {
            requireModel(model);
            if (journal.exists()) throw new IOException("Recovery journal exists; restart app");
            for (Params.Write w : writes) {
                if (w.id == Params.ID_QFMT || w.id == Params.ID_QFMT2 || w.id == Params.ID_QJPG || w.id == Params.ID_QJPG2)
                    throw new IOException("Set image quality in the camera MENU");
                if (old.containsKey(w.id)) throw new IOException("Duplicate slot");
                old.put(w.id, read(w.id));
                int attr = store.attr(w.id);
                if (attr < 0 || Params.slotLocked(attr)) throw new IOException("Slot unavailable/read-only: " + Integer.toHexString(w.id));
            }
            if (old.isEmpty()) return;
            byte[] data = encode(old);
            writeFile(journal, data, true);
            if (!Arrays.equals(data, readFile(journal, 2048))) throw new IOException("Journal verification failed");
            started = true;
            for (Params.Write w : writes) {
                // Recheck originals to catch a camera setting changed after the preflight.
                if (!Arrays.equals(old.get(w.id), read(w.id))) throw new IOException("Camera settings changed; retry");
                byte[] value = { (byte) w.value };
                store.write(w.id, value);
                if (!Arrays.equals(value, read(w.id))) throw new IOException("Write verification failed: " + Integer.toHexString(w.id));
            }
            store.sync();
            for (Params.Write w : writes) if ((read(w.id)[0] & 255) != (w.value & 255)) throw new IOException("Sync verification failed");
            if (!journal.delete()) throw new IOException("Cannot commit journal");
        } catch (Exception e) {
            if (!started) {
                // A preflight/journal failure has not changed the camera. A bad journal still blocks future writes.
                throw new Failure("Not applied: " + e.getMessage(), journal.exists());
            }
            try {
                restore(old);
                if (!journal.delete()) throw new IOException("Cannot clear journal");
            } catch (Exception rollback) {
                throw new Failure("STOP: recovery required. " + e.getMessage() + "; " + rollback.getMessage(), true);
            }
            throw new Failure("Not applied; original settings restored. " + e.getMessage(), false);
        }
    }
    private void restore(LinkedHashMap<Integer, byte[]> old) throws Exception {
        // Validate every destination BEFORE restoring anything. Do not guess if a slot cannot be read.
        for (Integer id : old.keySet()) {
            read(id);
            int attr = store.attr(id);
            if (attr < 0 || Params.slotLocked(attr)) throw new IOException("Recovery slot unavailable: " + Integer.toHexString(id));
        }
        for (Map.Entry<Integer, byte[]> e : old.entrySet()) store.write(e.getKey(), e.getValue());
        store.sync();
        for (Map.Entry<Integer, byte[]> e : old.entrySet()) if (!Arrays.equals(e.getValue(), read(e.getKey()))) throw new IOException("Recovery verification failed");
    }
    private static byte[] encode(LinkedHashMap<Integer, byte[]> old) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(MAGIC); out.writeUTF(MODEL); out.writeInt(old.size());
        for (Map.Entry<Integer, byte[]> e : old.entrySet()) { out.writeInt(e.getKey()); out.writeByte(e.getValue()[0]); }
        out.flush(); byte[] payload = bytes.toByteArray();
        out.write(MessageDigest.getInstance("SHA-256").digest(payload)); out.flush();
        return bytes.toByteArray();
    }
    private static LinkedHashMap<Integer, byte[]> decode(byte[] bytes) throws Exception {
        if (bytes.length < 32) throw new IOException("Invalid recovery journal");
        byte[] payload = Arrays.copyOf(bytes, bytes.length - 32);
        if (!Arrays.equals(MessageDigest.getInstance("SHA-256").digest(payload), Arrays.copyOfRange(bytes, bytes.length - 32, bytes.length))) throw new IOException("Journal checksum mismatch");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
        if (in.readInt() != MAGIC) throw new IOException("Invalid journal version");
        requireModel(in.readUTF()); int count = in.readInt();
        if (count < 1 || count > Params.allSlots().size()) throw new IOException("Invalid journal count");
        LinkedHashMap<Integer, byte[]> old = new LinkedHashMap<Integer, byte[]>();
        for (int i = 0; i < count; i++) {
            int id = in.readInt();
            if (!Params.allSlots().contains(id) || old.containsKey(id)) throw new IOException("Invalid journal slot");
            old.put(id, new byte[] { in.readByte() });
        }
        if (in.available() != 0) throw new IOException("Invalid journal length");
        return old;
    }
    static byte[] readFile(File f, int max) throws IOException {
        if (f.length() > max) throw new IOException("File too large");
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[512]; int n;
            while ((n = in.read(b)) != -1) { if (out.size() + n > max) throw new IOException("File too large"); out.write(b, 0, n); }
            return out.toByteArray();
        } finally { in.close(); }
    }
    static void writeFile(File f, byte[] bytes, boolean requireSync) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        try { out.write(bytes); out.flush(); try { out.getFD().sync(); } catch (IOException e) { if (requireSync) throw e; } }
        finally { out.close(); }
    }
}
