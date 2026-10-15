package com.voxivoid.recipelab;

/** Shared camera UI tokens and deterministic list geometry; no camera/settings access. */
final class SonyUi {
    private SonyUi() {}
    // Local adaptation of the public Pro-UI examples, not official Sony token values.
    static final int ACCENT = 0xFF46A9DF, TEXT = 0xFFF2F2F2, MUTED = 0xFFB0B0B0;
    static final int BACKGROUND = 0xFF282828, HEADER = 0xFF303030, RULE = 0xFF505050;
    static final int TRACK = 0xFF505050, SELECTED = 0xFF334450;
    static final int ON_ACCENT = 0xFF101820;
    // Local camera-inspired semantic colours, not claimed Sony/ZEISS official swatches.
    static final int ENABLED = 0xFF45B8B2, ATTENTION = 0xFFF28A5B;
    // Logical px in a 320x240 viewport. Roles, not per-screen guesses.
    static final int TITLE = 14, BODY = 12, SECONDARY = 10, INPUT = 16, NUMBER = 24, KEY_LETTER = 8;
    static final int ROW_HEIGHT = 28, INFO_ROW_HEIGHT = 24, DIALOG_WIDTH = 256;
    static final int ICON = 14, ICON_STROKE = 1, KEY_SIZE = 12, KEY_GAP = 4, ITEM_GAP = 8;
    static final int FOOTER_BOTTOM = 8, FOOTER_GAP = 6, LIST_GAP = 8;
    static final int LIVE_HEIGHT = 72, LIVE_QUIET_HEIGHT = 54, LIVE_HINT_TOP = 52;
    static final int PAGE_PAD = 12, HEADER_HEIGHT = 28, BADGE_HEIGHT = 16, BADGE_PAD = 6;
    static int statusFrame(boolean applied) { return applied ? ENABLED : MUTED; }
    static int statusText(boolean applied) { return applied ? ON_ACCENT : MUTED; }

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
