package com.voxivoid.recipelab;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Shared camera chrome. Layout/paint only: no input routing, persistence or hardware access. */
final class UiChrome {
    private final float d;
    private final Paint header=new Paint(),rule=new Paint(),fill=new Paint(),edge=new Paint(),marker=new Paint();
    UiChrome(float scale){
        d=scale;header.setColor(SonyUi.HEADER);rule.setColor(SonyUi.RULE);rule.setStrokeWidth(d);
        fill.setColor(SonyUi.SELECTED);edge.setColor(SonyUi.ACCENT);edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(d);
        marker.setColor(SonyUi.ACCENT);
    }
    void header(Canvas c,float width,String text,Paint title){
        float pad=SonyUi.PAGE_PAD*d,h=SonyUi.HEADER_HEIGHT*d;
        c.drawRect(0,0,width,h,header);
        title.setColor(SonyUi.TEXT);
        c.drawText(CanvasText.fit(title,text,width-2*pad),pad,CanvasText.baseline(title,h/2),title);
        separator(c,pad,h,width-pad);
    }
    float footerTop(float width,float height,Legend legend,Keys.Hints hints){
        return height-SonyUi.FOOTER_BOTTOM*d-legend.height(legend.lines(width-2*SonyUi.PAGE_PAD*d,hints));
    }
    void footer(Canvas c,float width,float top,Legend legend,Keys.Hints hints){
        float pad=SonyUi.PAGE_PAD*d;
        separator(c,pad,top-SonyUi.FOOTER_GAP*d/2,width-pad);
        legend.drawWrapped(c,pad,top,width-2*pad,hints);
    }
    void separator(Canvas c,float left,float y,float right){c.drawLine(left,y,right,y,rule);}
    void selection(Canvas c,float left,float top,float right,float bottom,boolean active){
        c.drawRect(left,top,right,bottom,fill);
        if(active){c.drawRect(left,top,right,bottom,edge);c.drawRect(left,top,left+2*d,bottom,marker);}
    }
    float status(Canvas c,float right,float cy,Paint text,boolean applied){
        float width=CanvasText.statusWidth(text,d),left=right-width,h=SonyUi.BADGE_HEIGHT*d/2;
        header.setColor(SonyUi.statusFrame(applied));header.setStyle(applied?Paint.Style.FILL:Paint.Style.STROKE);header.setStrokeWidth(d);
        c.drawRect(left,cy-h,right,cy+h,header);
        header.setStyle(Paint.Style.FILL);header.setColor(SonyUi.HEADER);
        String label=Lang.t(applied?"state_active":"state_preview");text.setColor(SonyUi.statusText(applied));
        c.drawText(label,left+(width-text.measureText(label))/2,CanvasText.baseline(text,cy),text);
        return left;
    }
    float tag(Canvas c,float right,float cy,String label,Paint text){
        float width=text.measureText(label)+12*d,left=right-width,h=SonyUi.BADGE_HEIGHT*d/2;
        c.drawRect(left,cy-h,right,cy+h,header);text.setColor(SonyUi.MUTED);
        c.drawText(label,left+6*d,CanvasText.baseline(text,cy),text);return left;
    }
}
