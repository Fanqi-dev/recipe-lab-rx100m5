package com.voxivoid.recipelab;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** 8.3-compatible card save journal. Preserve the original until the new file is verified. */
final class SafeRecipeFiles {
    static final String JOURNAL = "SAVE.JRN", BACKUP = "SAVING.BAK", TEMP = "SAVING.TMP";
    interface Ops { boolean rename(File from, File to); }
    static final Ops NORMAL = new Ops() { public boolean rename(File a, File b) { return a.renameTo(b); } };
    private static String hash(byte[] bytes) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(bytes); StringBuilder s = new StringBuilder();
        for (byte b : h) s.append(String.format(java.util.Locale.US, "%02x", b & 255)); return s.toString();
    }
    private static boolean valid(String name) { return name != null && name.matches("[A-Z0-9]{1,8}\\.YML"); }
    static void recover(File dir) throws IOException { recover(dir, NORMAL); }
    static void recover(File dir, Ops ops) throws IOException {
        File journal = new File(dir, JOURNAL), backup = new File(dir, BACKUP), tmp = new File(dir, TEMP);
        if (!journal.exists()) {
            if (backup.exists()) throw new IOException("SAVING.BAK needs manual recovery; no journal");
            return; // An abandoned TMP is overwritten only when the next valid save starts.
        }
        try {
            String[] p = new String(SafeSettings.readFile(journal, 512), "UTF-8").split("\n");
            if (p.length != 5 || !"RX5SAVE1".equals(p[0]) || !(p[1].equals("-") || valid(p[1])) || !valid(p[2]) || !p[3].matches("[0-9a-f]{64}")) throw new IOException("Invalid save journal; preserve files");
            String header = p[0]+"\n"+p[1]+"\n"+p[2]+"\n"+p[3]+"\n";
            if (!hash(header.getBytes("UTF-8")).equals(p[4])) throw new IOException("Save journal checksum mismatch");
            File target = new File(dir, p[2]), original = p[1].equals("-") ? null : new File(dir, p[1]);
            boolean complete = target.isFile() && hash(SafeSettings.readFile(target, CustomRecipes.MAX_BYTES)).equals(p[3]);
            if (complete) { remove(backup); remove(tmp); remove(journal); return; }
            if (backup.exists()) {
                if (original == null || original.exists() || target.exists()) throw new IOException("Conflicting save files; preserve all");
                if (!ops.rename(backup, original)) throw new IOException("Cannot restore original recipe");
            } else if (original != null && !original.isFile()) throw new IOException("Original recipe unavailable; preserve journal");
            // No verified new file: recover the original and discard only our known temporary file.
            remove(tmp); remove(journal);
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("Recipe recovery: " + e.getMessage()); }
    }
    static void save(File dir, String targetName, String oldName, byte[] bytes) throws IOException { save(dir,targetName,oldName,bytes,NORMAL); }
    static void save(File dir, String targetName, String oldName, byte[] bytes, Ops ops) throws IOException {
        if (!valid(targetName) || oldName != null && !valid(oldName)) throw new IOException("Unsupported recipe filename");
        recover(dir, ops);
        File target = new File(dir,targetName), original = oldName == null ? null : new File(dir,oldName);
        File tmp = new File(dir,TEMP), backup = new File(dir,BACKUP), journal = new File(dir,JOURNAL);
        if (target.exists() && (original == null || !target.getCanonicalPath().equalsIgnoreCase(original.getCanonicalPath()))) throw new IOException("Recipe destination exists");
        if (original != null && !original.isFile()) throw new IOException("Original recipe missing");
        try {
            SafeSettings.writeFile(tmp,bytes,false);
            if (!Arrays.equals(bytes,SafeSettings.readFile(tmp,CustomRecipes.MAX_BYTES))) throw new IOException("Temporary recipe verification failed");
            String header="RX5SAVE1\n"+(oldName == null ? "-" : oldName)+"\n"+targetName+"\n"+hash(bytes)+"\n";
            byte[] record=(header+hash(header.getBytes("UTF-8"))+"\n").getBytes("UTF-8");
            SafeSettings.writeFile(journal,record,false);
            if (!Arrays.equals(record,SafeSettings.readFile(journal,512))) throw new IOException("Save journal verification failed");
            if (original != null && !ops.rename(original,backup)) throw new IOException("Cannot preserve original recipe");
            if (!ops.rename(tmp,target)) throw new IOException("Cannot install new recipe");
            if (!Arrays.equals(bytes,SafeSettings.readFile(target,CustomRecipes.MAX_BYTES))) throw new IOException("Saved recipe verification failed");
            remove(backup); remove(journal);
        } catch (Exception e) {
            try { recover(dir,ops); } catch (IOException recovery) { throw new IOException(e.getMessage()+"; "+recovery.getMessage()); }
            throw new IOException(e.getMessage());
        }
    }
    private static void remove(File file) throws IOException { if (file.exists() && !file.delete()) throw new IOException("Cannot remove " + file.getName()); }
}
