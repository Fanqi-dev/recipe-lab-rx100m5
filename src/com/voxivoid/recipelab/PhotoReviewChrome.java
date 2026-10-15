package com.voxivoid.recipelab;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Only the review footer. The photo is displayed separately by Sony's OptimizedImageView. */
public final class PhotoReviewChrome extends View {
    static final int HEIGHT=38;
    private final float d;
    private final Paint bg=new Paint(),line=new Paint(),title=new Paint(Paint.ANTI_ALIAS_FLAG),
            detail=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String text="",zoom="";
    private boolean enlarged;
    private final Legend legend;
    public PhotoReviewChrome(Context context,AttributeSet attrs) {
        super(context,attrs);d=UiScale.of(context);
        legend=new Legend(d);bg.setColor(SonyUi.BACKGROUND);line.setColor(SonyUi.RULE);
        title.setColor(SonyUi.TEXT);title.setTextSize(SonyUi.BODY*d);
        detail.setColor(SonyUi.MUTED);detail.setTextSize(SonyUi.SECONDARY*d);
    }
    public void setTypeface(Typeface face){title.setTypeface(face);detail.setTypeface(face);legend.setTypeface(face);invalidate();}
    public void set(String text,float zoom){this.text=text;enlarged=zoom>1;this.zoom=enlarged?((int)zoom)+"x":"";
        setContentDescription(text+"  "+Lang.t(enlarged?"review_zoom_keys":"review_keys"));invalidate();}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),(int)Math.ceil(HEIGHT*d));}
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),pad=SonyUi.PAGE_PAD*d,zw=detail.measureText(zoom);
        c.drawRect(0,0,w,getHeight(),bg);c.drawLine(0,0,w,0,line);
        c.drawText(CanvasText.fit(title,text,w-2*pad-zw-10*d),pad,CanvasText.baseline(title,11*d),title);
        detail.setColor(SonyUi.MUTED);c.drawText(zoom,w-pad-zw,CanvasText.baseline(detail,11*d),detail);
        legend.drawWrapped(c,pad,20*d,w-2*pad,Keys.hints(enlarged?Keys.H_REVIEW_ZOOM:Keys.H_REVIEW,Keys.Caps.UNKNOWN));
    }
}
