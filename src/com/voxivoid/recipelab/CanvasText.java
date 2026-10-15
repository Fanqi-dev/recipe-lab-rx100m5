package com.voxivoid.recipelab;
import android.graphics.Paint;

/** Keep translated labels and user recipe names inside their own column. */
final class CanvasText {
    private CanvasText() {}
    /** A shared visual centre for labels of different sizes, using the actual selected font. */
    static float baseline(Paint paint, float centerY) {
        return centerY - (paint.ascent() + paint.descent()) / 2f;
    }
    /** Both states occupy the same width in a language; switching state never shifts the frame. */
    static float statusWidth(Paint paint, float scale) {
        return Math.max(paint.measureText(Lang.t("state_active")),
                paint.measureText(Lang.t("state_preview"))) + 2 * SonyUi.BADGE_PAD * scale;
    }
    static String fit(Paint paint, String text, float width) {
        if (text == null || width <= 0) return "";
        if (paint.measureText(text) <= width) return text;
        String dots = "...";
        if (paint.measureText(dots) > width) return "";
        int end = paint.breakText(text, true, width - paint.measureText(dots), null);
        return text.substring(0, SonyUi.safeEnd(text, end)) + dots;
    }
}
