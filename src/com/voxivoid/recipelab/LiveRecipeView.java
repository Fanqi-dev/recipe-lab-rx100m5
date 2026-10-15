package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Compact, layered shooting information. No decoding, camera access or settings writes. */
public final class LiveRecipeView extends View {
    private final float d;
    private final Paint surface = new Paint(), rule = new Paint(), focus = new Paint(),
            title = new Paint(Paint.ANTI_ALIAS_FLAG), state = new Paint(Paint.ANTI_ALIAS_FLAG),
            small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Legend legend;
    private final UiChrome chrome;
    private Keys.Hints hints = Keys.hints(Keys.H_LIVE,Keys.Caps.UNKNOWN);
    private String name = "", group = "", position = "";
    private String[] summaryFields = new String[0];
    private boolean applied, edited, showHints = true;

    public LiveRecipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        d = UiScale.of(context);
        legend = new Legend(d);chrome=new UiChrome(d);
        surface.setColor(SonyUi.BACKGROUND); rule.setColor(SonyUi.RULE);
        focus.setColor(SonyUi.ACCENT);
        title.setColor(SonyUi.TEXT); title.setTextSize(SonyUi.TITLE * d);
        state.setTextSize(SonyUi.SECONDARY * d);
        small.setColor(SonyUi.MUTED); small.setTextSize(SonyUi.SECONDARY * d);
    }
    public void setTypeface(Typeface face) {
        title.setTypeface(face); state.setTypeface(face); small.setTypeface(face); legend.setTypeface(face);
        requestLayout(); invalidate();
    }
    public void set(String name, String group, String position, String summary,
            boolean applied, boolean edited, Keys.Caps caps, boolean showHints) {
        this.name = name; this.group = group; this.position = position; summaryFields=summary.split("   ",-1);
        this.applied = applied; this.edited = edited;
        this.showHints = showHints;
        hints = Keys.hints(Keys.H_LIVE,caps);
        requestLayout(); invalidate();
    }
    /** Summarise the displayed staged values, not the recipe defaults or unverified camera state. */
    static String summary(int[] values) {
        int look = values[Params.R_PE] == 0 ? Params.R_STYLE : Params.R_PE;
        int wb = values[Params.R_WBMODE] == Params.WB_KELVIN ? Params.R_KELVIN : Params.R_WBMODE;
        return Params.fmt(look,values[look],values) + "   WB " + Params.fmt(wb,values[wb],values)
                + "   EV " + Params.fmt(Params.R_EV,values[Params.R_EV],values);
    }
    @Override protected void onMeasure(int width, int height) {
        int w=MeasureSpec.getSize(width);
        float h=SonyUi.LIVE_QUIET_HEIGHT*d;
        if(showHints)h=Math.max(SonyUi.LIVE_HEIGHT*d,SonyUi.LIVE_HINT_TOP*d+legend.height(legend.lines(Math.max(1,w-2*SonyUi.PAGE_PAD*d),hints))+2*d);
        setMeasuredDimension(w,(int)Math.ceil(h));
    }
    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), pad = SonyUi.PAGE_PAD*d, badgeWidth = CanvasText.statusWidth(state,d);
        float left = w - pad - badgeWidth;
        float cy = 27*d;
        c.drawRect(0,0,w,getHeight(),surface);
        c.drawLine(0,0,w,0,rule);
        float countWidth=small.measureText(position);
        String editedLabel=edited?Lang.t("state_edited"):"";
        float editedWidth=edited?small.measureText(editedLabel)+12*d:0;
        small.setColor(SonyUi.MUTED);
        c.drawText(CanvasText.fit(small,group,w-2*pad-countWidth-12*d-editedWidth),pad,CanvasText.baseline(small,9.5f*d),small);
        c.drawText(position,w-pad-countWidth,CanvasText.baseline(small,9.5f*d),small);
        if(edited){small.setColor(SonyUi.ATTENTION);c.drawText(editedLabel,w-pad-countWidth-12*d-small.measureText(editedLabel),CanvasText.baseline(small,9.5f*d),small);small.setColor(SonyUi.MUTED);}
        c.drawRect(pad-6*d,cy-8*d,pad-4*d,cy+8*d,focus);
        chrome.status(c,w-pad,cy,state,applied);
        // A filled green badge and an outlined neutral badge differ in shape as well as colour.
        String fitted = CanvasText.fit(title,name,left-pad-8*d);
        title.setColor(SonyUi.TEXT);
        float titleBase=CanvasText.baseline(title,cy);
        c.drawText(fitted,pad,titleBase,title);
        drawSummary(c,pad,45*d,w-2*pad);
        if(showHints)legend.drawWrapped(c,pad,SonyUi.LIVE_HINT_TOP*d,w-2*pad,hints);
    }
    private void drawSummary(Canvas c,float x,float cy,float width){
        String[] fields=summaryFields;
        float natural=0;for(String field:fields)natural+=small.measureText(field);
        float gap=SonyUi.ITEM_GAP*d;
        float maxText=Math.max(0,width-2*gap*(fields.length-1));
        for(int i=0;i<fields.length;i++){
            if(i>0){c.drawLine(x+gap,cy-4*d,x+gap,cy+4*d,rule);x+=2*gap;}
            float own=small.measureText(fields[i]);
            float available=natural>maxText&&natural>0?own*maxText/natural:own;
            String text=CanvasText.fit(small,fields[i],available+.1f);
            c.drawText(text,x,CanvasText.baseline(small,cy),small);x+=available;
        }
    }
}
