package com.voxivoid.recipelab;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

/** Read-only DCIM inventory. No recursion, deletion, renaming, or media database writes. */
final class ReviewIndex {
    static final int LIMIT = 500;
    static final long MAX_BYTES = 32L * 1024 * 1024;
    interface Cancel { boolean stopped(); }
    static final class Photo {
        final File file; final long modified;
        Photo(File f, long t) { file = f; modified = t; }
    }
    private static final Comparator<Photo> ORDER = new Comparator<Photo>() {
        public int compare(Photo a, Photo b) {
            int t = a.modified < b.modified ? -1 : a.modified > b.modified ? 1 : 0;
            return t != 0 ? t : a.file.getPath().compareTo(b.file.getPath());
        }
    };
    static List<Photo> scan(File card, Cancel cancel) throws IOException {
        if (card == null) throw new IOException("No memory card");
        File root = new File(card, "DCIM").getCanonicalFile();
        if (!root.getParentFile().equals(card.getCanonicalFile())) throw new IOException("DCIM points outside memory card");
        if (!root.isDirectory()) throw new IOException("No DCIM folder");
        PriorityQueue<Photo> newest = new PriorityQueue<Photo>(LIMIT + 1, ORDER);
        File[] dirs = root.listFiles();
        if (dirs == null) throw new IOException("Cannot read DCIM");
        for (File dir : dirs) {
            if (cancel.stopped()) return Collections.emptyList();
            File real = dir.getCanonicalFile();
            if (!real.getParentFile().equals(root)) continue;
            if (real.isFile()) add(real, newest);
            else if (real.isDirectory()) {
                File[] files = real.listFiles();
                if (files == null) continue;
                for (File f : files) {
                    if (cancel.stopped()) return Collections.emptyList();
                    File p = f.getCanonicalFile();
                    if (p.getParentFile().equals(real)) add(p, newest);
                }
            }
        }
        List<Photo> result = new ArrayList<Photo>(newest);
        Collections.sort(result, Collections.reverseOrder(ORDER));
        return result;
    }
    private static void add(File f, PriorityQueue<Photo> photos) {
        String name = f.getName().toLowerCase(Locale.US);
        long length = f.length();
        if (!f.isFile() || !(name.endsWith(".jpg") || name.endsWith(".jpeg")) || length <= 0 || length > MAX_BYTES) return;
        photos.add(new Photo(f, f.lastModified())); // stat once, never inside the sort comparator
        if (photos.size() > LIMIT) photos.poll();
    }
    static int sample(int width, int height) throws IOException {
        if (width <= 0 || height <= 0 || width > 24000 || height > 24000 || (long) width * height > 300000000L)
            throw new IOException("Invalid JPEG dimensions");
        int n = 1;
        while ((width + n - 1L) / n > 800 || (height + n - 1L) / n > 800) n *= 2;
        return n;
    }
    static int next(int index, int direction, int count) {
        return count <= 0 ? -1 : (index + count + (direction > 0 ? 1 : -1)) % count;
    }
}
