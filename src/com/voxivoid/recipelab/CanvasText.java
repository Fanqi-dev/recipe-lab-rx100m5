package com.voxivoid.recipelab;
import android.graphics.Paint;

/** Keep translated labels and user recipe names inside their own column. */
final class CanvasText {
    private CanvasText() {}
    static String fit(Paint paint, String text, float width) {
        if (text == null || width <= 0) return "";
        if (paint.measureText(text) <= width) return text;
        String dots = "...";
        if (paint.measureText(dots) > width) return "";
        int end = paint.breakText(text, true, width - paint.measureText(dots), null);
        return text.substring(0, SonyUi.safeEnd(text, end)) + dots;
    }
}
