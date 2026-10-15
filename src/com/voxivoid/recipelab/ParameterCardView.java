package com.voxivoid.recipelab;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Focused parameter editor; changing a displayed value never confirms a permanent write. */
public final class ParameterCardView extends View {
    private final float d;
    private final Legend legend;
    private final Paint bg=new Paint(), edge=new Paint(), title=new Paint(Paint.ANTI_ALIAS_FLAG),
            value=new Paint(Paint.ANTI_ALIAS_FLAG), muted=new Paint(Paint.ANTI_ALIAS_FLAG), focus=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tri=new Path();
    private String label="", text="";
    private Keys.Hints hints=Keys.hints(Keys.H_EDIT,Keys.Caps.UNKNOWN);
    public ParameterCardView(Context context,AttributeSet attrs) {
        super(context,attrs); d=UiScale.of(context); legend=new Legend(d);
        bg.setColor(SonyUi.BACKGROUND); edge.setColor(SonyUi.RULE); focus.setColor(SonyUi.ACCENT);
        title.setColor(SonyUi.TEXT); title.setTextSize(SonyUi.TITLE*d);
        value.setColor(SonyUi.TEXT); value.setTextSize(SonyUi.NUMBER*d);
        muted.setColor(SonyUi.MUTED); muted.setTextSize(SonyUi.SECONDARY*d);
    }
    public void setTypeface(Typeface face) {
        title.setTypeface(face); value.setTypeface(face); muted.setTypeface(face); legend.setTypeface(face);
        requestLayout(); invalidate();
    }
    public void set(String label,String value) {
        this.label=label; text=value; hints=Keys.hints(Keys.H_EDIT,Keys.Caps.UNKNOWN); requestLayout(); invalidate();
    }
    @Override protected void onMeasure(int w,int h) {
        int width=(int)Math.min(SonyUi.DIALOG_WIDTH*d,Math.max(1,MeasureSpec.getSize(w)-32*d));
        setMeasuredDimension(width,(int)(94*d+legend.height(legend.lines(width-24*d,hints))+10*d));
    }
    @Override protected void onDraw(Canvas c) {
        float w=getWidth(),h=getHeight(),pad=SonyUi.PAGE_PAD*d;
        c.drawRect(0,0,w,h,bg);
        c.drawLine(0,0,w,0,edge);c.drawLine(0,h-1,w,h-1,edge);c.drawLine(0,0,0,h,edge);c.drawLine(w-1,0,w-1,h,edge);
        String preview=Lang.t("action_edit"); float sw=muted.measureText(preview);
        c.drawText(CanvasText.fit(title,label,w-2*pad-sw-14*d),pad,CanvasText.baseline(title,17*d),title);
        c.drawText(preview,w-pad-sw,CanvasText.baseline(muted,17*d),muted);
        c.drawLine(pad,30*d,w-pad,30*d,edge);
        String shown=CanvasText.fit(value,text,w-80*d);
        c.drawText(shown,(w-value.measureText(shown))/2,CanvasText.baseline(value,58*d),value);
        triangle(c,20*d,58*d,25*d,53*d,25*d,63*d);
        triangle(c,w-20*d,58*d,w-25*d,53*d,w-25*d,63*d);
        // No decorative underline that could be read as a value/progress slider.
        c.drawLine(pad,88*d,w-pad,88*d,edge);
        legend.drawWrapped(c,pad,94*d,w-2*pad,hints);
    }
    private void triangle(Canvas c,float x,float y,float a,float b,float e,float f) {
        tri.reset();tri.moveTo(x,y);tri.lineTo(a,b);tri.lineTo(e,f);tri.close();c.drawPath(tri,focus);
    }
}
