package com.voxivoid.recipelab;

/** Shared camera UI tokens and deterministic list geometry; no camera/settings access. */
final class SonyUi {
    private SonyUi() {}
    // Local adaptation of the public Pro-UI examples, not official Sony token values.
    static final int ACCENT = 0xFF46A9DF, TEXT = 0xFFF2F2F2, MUTED = 0xFFB0B0B0;
    static final int BACKGROUND = 0xFF282828, HEADER = 0xFF303030, RULE = 0xFF505050;
    static final int TRACK = 0xFF505050, SELECTED = 0xFF334450;
    static final int ON_ACCENT = 0xFF101820;

    static boolean compactPanel(boolean ready, int row, boolean actions, boolean warning) {
        return ready && row == 0 && !actions && !warning;
    }
    static float scale(float density, int width, int height) {
        if (width <= 0 || height <= 0) return density > 0 ? density : 1;
        return Math.max(1,Math.min(3,Math.min(width/320f,height/240f)));
    }

    static int visibleRows(float available, float rowHeight) {
        return available <= 0 || rowHeight <= 0 ? 0 : Math.max(0, (int) (available / rowHeight));
    }
    static int firstRow(int total, int visible, int selected) {
        if (total <= visible || visible <= 0 || selected < 0) return 0;
        return Math.max(0, Math.min(selected - visible / 2, total - visible));
    }
    static int safeEnd(String text, int count) {
        int end = Math.max(0, Math.min(count, text.length()));
        if (end > 0 && end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return end;
    }
}
