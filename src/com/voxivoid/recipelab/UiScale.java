package com.voxivoid.recipelab;
import android.content.Context;
import android.util.DisplayMetrics;
/** Canvas layout uses a camera-sized logical viewport rather than assuming the firmware's DPI. */
final class UiScale {
    private UiScale() {}
    static float of(Context context) {
        DisplayMetrics m=context.getResources().getDisplayMetrics();
        return SonyUi.scale(m.density,m.widthPixels,m.heightPixels);
    }
}
