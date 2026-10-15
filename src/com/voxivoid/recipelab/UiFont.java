package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.TypedValue;

/**
 * The typeface each display language ({@link Lang}) is drawn in. English keeps the camera's own font. Chinese needs
 * the fonts in assets/fonts — the firmware font has no CJK glyphs — which tools/subset-font.py cuts down to what the
 * tables use, one per script so each gets its own glyph forms. A font that will not load falls back to the camera's:
 * the text is then boxes, but the app still opens.
 */
final class UiFont {
    private UiFont() {}

    private static final String[] ASSET = { null, "fonts/RecipeLabCJKsc-Regular.ttf", "fonts/RecipeLabCJKtc-Regular.ttf" };
    private static final Typeface[] LOADED = new Typeface[Lang.COUNT];

    /** the typeface of a language, loaded the first time it is asked for */
    static Typeface of(Context c, int lang) {
        if (ASSET[lang] == null) return Typeface.DEFAULT;
        if (LOADED[lang] == null) {
            try { LOADED[lang] = Typeface.createFromAsset(c.getAssets(), ASSET[lang]); }
            catch (Throwable t) { LOADED[lang] = Typeface.DEFAULT; }
        }
        return LOADED[lang];
    }

    /**
     * Every TextView uses the same regular face as Canvas text. Hierarchy comes from size and colour,
     * not synthetic bold that thickens Latin and Chinese differently.
     */
    static void apply(View v, Typeface tf) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            t.setTypeface(tf, Typeface.NORMAL);
            t.setIncludeFontPadding(false);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) apply(g.getChildAt(i), tf);
        }
    }
    static void text(TextView view,int logicalSize){
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,logicalSize*UiScale.of(view.getContext()));
        view.setIncludeFontPadding(false);
    }
    /** Once, after XML inflation: put dp-based legacy containers on the Canvas viewport grid. */
    static void cameraLayout(View view){
        float density=view.getResources().getDisplayMetrics().density;
        float ratio=UiScale.of(view.getContext())/(density>0?density:1);
        scaleLayout(view,ratio);
    }
    private static void scaleLayout(View view,float ratio){
        view.setPadding(Math.round(view.getPaddingLeft()*ratio),Math.round(view.getPaddingTop()*ratio),
                Math.round(view.getPaddingRight()*ratio),Math.round(view.getPaddingBottom()*ratio));
        ViewGroup.LayoutParams lp=view.getLayoutParams();
        if(lp!=null){
            if(lp.width>0)lp.width=Math.round(lp.width*ratio);
            if(lp.height>0)lp.height=Math.round(lp.height*ratio);
            if(lp instanceof ViewGroup.MarginLayoutParams){
                ViewGroup.MarginLayoutParams m=(ViewGroup.MarginLayoutParams)lp;
                m.leftMargin=Math.round(m.leftMargin*ratio);m.topMargin=Math.round(m.topMargin*ratio);
                m.rightMargin=Math.round(m.rightMargin*ratio);m.bottomMargin=Math.round(m.bottomMargin*ratio);
            }
            view.setLayoutParams(lp);
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)scaleLayout(group.getChildAt(i),ratio);}
    }
}
