package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Compact shooting HUD. Presentation only; key handling and all writes remain in MainActivity. */
public final class LiveRecipeView extends View {
    private final float d;
    private final Paint surface = new Paint(), rule = new Paint(), title = new Paint(Paint.ANTI_ALIAS_FLAG),
            detail = new Paint(Paint.ANTI_ALIAS_FLAG), state = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Legend legend;
    private String name = "", group = "", position = "", summary = "", status = "";
    private boolean applied, quiet;
    private Keys.Caps caps = Keys.Caps.UNKNOWN;
    private Keys.Hints hints = Keys.hints(Keys.H_LIVE, caps);

    public LiveRecipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        d = UiScale.of(context);
        legend = new Legend(d);
        surface.setColor(SonyUi.BACKGROUND); rule.setColor(SonyUi.RULE);
        title.setColor(SonyUi.TEXT); title.setTextSize(17 * d);
        detail.setColor(SonyUi.MUTED); detail.setTextSize(11 * d);
        state.setColor(SonyUi.TEXT); state.setTextSize(11 * d);
    }
    public void setTypeface(Typeface face) {
        title.setTypeface(face); detail.setTypeface(face); state.setTypeface(face); legend.setTypeface(face);
        requestLayout(); invalidate();
    }
    public void setCaps(Keys.Caps value) { caps = value; hints = Keys.hints(Keys.H_LIVE,caps); requestLayout(); invalidate(); }
    public void set(String name, String group, String position, String summary, boolean applied, boolean edited, boolean quiet) {
        this.name=name; this.group=group; this.position=position; this.summary=summary;
        this.applied=applied; this.quiet=quiet;
        status=Lang.t(applied ? "state_active" : "state_preview") + (edited ? " / " + Lang.t("state_edited") : "");
        hints = Keys.hints(Keys.H_LIVE,caps);
        requestLayout(); invalidate();
    }
    @Override protected void onMeasure(int width, int height) {
        int w=MeasureSpec.getSize(width);
        float guide=quiet ? 0 : legend.height(legend.lines(w - 24*d,hints)) + 6*d;
        setMeasuredDimension(w, (int)Math.ceil(61*d + guide));
    }
    @Override protected void onDraw(Canvas c) {
        float w=getWidth(), pad=12*d;
        c.drawRect(0,0,w,getHeight(),surface);
        c.drawLine(0,0,w,0,rule);
        float posWidth=detail.measureText(position);
        c.drawText(CanvasText.fit(detail,group,w-2*pad-posWidth-12*d),pad,15*d,detail);
        c.drawText(position,w-pad-posWidth,15*d,detail);
        float stateWidth=state.measureText(status), checkWidth=applied?14*d:0;
        c.drawText(CanvasText.fit(title,name,w-2*pad-stateWidth-checkWidth-16*d),pad,37*d,title);
        c.drawText(status,w-pad-stateWidth,36*d,state);
        if(applied) {
            rule.setColor(SonyUi.TEXT); rule.setStrokeWidth(d);
            float x=w-pad-stateWidth-11*d, y=32*d;
            c.drawLine(x-4*d,y,x-d,y+3*d,rule); c.drawLine(x-d,y+3*d,x+5*d,y-4*d,rule);
            rule.setColor(SonyUi.RULE);
        }
        // The focus marker is not the applied-state check mark.
        rule.setColor(SonyUi.ACCENT); c.drawRect(5*d,23*d,7*d,39*d,rule); rule.setColor(SonyUi.RULE);
        c.drawText(CanvasText.fit(detail,summary,w-2*pad),pad,54*d,detail);
        if(!quiet) legend.drawWrapped(c,pad,62*d,w-2*pad,hints);
    }
}
