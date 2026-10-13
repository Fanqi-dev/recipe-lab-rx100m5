package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.File;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** A bounded, single-worker JPEG viewer in the SAME activity: never launches native playback. */
final class PhotoReviewView extends FrameLayout {
    private final Handler ui = new Handler();
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1), new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Image image;
    private final TextView title, help;
    private List<ReviewIndex.Photo> photos;
    private int index;
    private volatile int generation;
    private volatile boolean open;
    private Future<?> pending;
    private Bitmap bitmap;
    PhotoReviewView(Context context) {
        super(context); setBackgroundColor(Color.BLACK); setVisibility(GONE);
        image = new Image(context); addView(image, new LayoutParams(-1, -1));
        title = text(context, Gravity.TOP); help = text(context, Gravity.BOTTOM);
        title.setTextSize(16); help.setTextSize(13);
        help.setText("LEFT / RIGHT / wheel: photo    MENU: return\nJPEG only / read-only / newest 500");
    }
    private TextView text(Context context, int gravity) {
        TextView t = new TextView(context); t.setTextColor(Color.WHITE); t.setBackgroundColor(0xBB000000);
        t.setPadding(8, 4, 8, 4);
        LayoutParams p = new LayoutParams(-1, -2, gravity); addView(t, p); return t;
    }
    void setTypeface(Typeface t) { title.setTypeface(t); help.setTypeface(t); }
    boolean isOpen() { return open; }
    void open(final File card) {
        close(); open = true; setVisibility(VISIBLE); title.setText("Loading JPEG photos...");
        final int ticket = ++generation;
        pending = worker.submit(new Runnable() {
            public void run() {
                try {
                    final List<ReviewIndex.Photo> found = ReviewIndex.scan(card, new ReviewIndex.Cancel() {
                        public boolean stopped() { return stale(ticket); }
                    });
                    ui.post(new Runnable() {
                        public void run() {
                            if (stale(ticket)) return;
                            photos = found; index = 0;
                            if (photos.isEmpty()) title.setText("No JPEG photos in DCIM. MENU: return");
                            else load();
                        }
                    });
                } catch (Exception e) { error(ticket, "Cannot read DCIM. MENU: return"); }
                catch (OutOfMemoryError e) { error(ticket, "Not enough memory. MENU: return"); }
            }
        });
    }
    void step(int direction) { if (photos != null && !photos.isEmpty()) { index = ReviewIndex.next(index, direction, photos.size()); load(); } }
    private boolean stale(int ticket) { return !open || ticket != generation || Thread.currentThread().isInterrupted(); }
    private void load() {
        if (pending != null) pending.cancel(true);
        final int ticket = ++generation;
        final File f = photos.get(index).file;
        title.setText((index + 1) + " / " + photos.size() + "   " + f.getName() + "   Loading...");
        // Drop the previous bitmap BEFORE decoding; never accumulate full-resolution images.
        releaseBitmap();
        pending = worker.submit(new Runnable() {
            public void run() {
                Bitmap decoded = null;
                try {
                    BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(f.getPath(), bounds);
                    if (stale(ticket)) return;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = ReviewIndex.sample(bounds.outWidth, bounds.outHeight);
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    decoded = BitmapFactory.decodeFile(f.getPath(), options);
                    if (decoded == null) throw new java.io.IOException("JPEG decode failed");
                    int orientation = 1;
                    try { orientation = new ExifInterface(f.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1); }
                    catch (Exception ignored) { }
                    if (stale(ticket)) { decoded.recycle(); return; }
                    final Bitmap result = decoded; final int direction = orientation;
                    ui.post(new Runnable() {
                        public void run() {
                            if (stale(ticket)) { result.recycle(); return; }
                            bitmap = result; image.orientation = direction; image.invalidate();
                            title.setText((index + 1) + " / " + photos.size() + "   " + f.getName());
                        }
                    });
                } catch (Exception e) { if (decoded != null) decoded.recycle(); error(ticket, "Cannot decode JPEG. LEFT/RIGHT: next; MENU: return"); }
                catch (OutOfMemoryError e) { if (decoded != null) decoded.recycle(); error(ticket, "Image too large. LEFT/RIGHT: next; MENU: return"); }
            }
        });
    }
    private void error(final int ticket, final String text) { ui.post(new Runnable() { public void run() { if (!stale(ticket)) title.setText(text); } }); }
    private void releaseBitmap() { Bitmap old = bitmap; bitmap = null; image.invalidate(); if (old != null) old.recycle(); }
    void close() { open = false; generation++; if (pending != null) pending.cancel(true); pending = null; photos = null; releaseBitmap(); setVisibility(GONE); }
    void dispose() { close(); worker.shutdownNow(); }
    private final class Image extends View {
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        int orientation = 1;
        Image(Context c) { super(c); }
        @Override protected void onDraw(Canvas c) {
            Bitmap b = bitmap; if (b == null || b.isRecycled()) return;
            boolean swap = orientation >= 5 && orientation <= 8;
            float w = swap ? b.getHeight() : b.getWidth(), h = swap ? b.getWidth() : b.getHeight();
            float scale = Math.min(getWidth() / w, Math.max(1, getHeight() - 100) / h);
            c.save(); c.translate(getWidth() / 2f, getHeight() / 2f); c.scale(scale, scale);
            switch (orientation) {
                case 2: c.scale(-1, 1); break;
                case 3: c.rotate(180); break;
                case 4: c.scale(1, -1); break;
                case 5: c.rotate(90); c.scale(1, -1); break;
                case 6: c.rotate(90); break;
                case 7: c.rotate(90); c.scale(-1, 1); break;
                case 8: c.rotate(-90); break;
            }
            c.drawBitmap(b, -b.getWidth() / 2f, -b.getHeight() / 2f, paint); c.restore();
        }
    }
}
