package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** Camera-style confirmation. Fixed type roles; descriptions wrap rather than becoming tiny. */
public class PromptView extends View {
    private final float d;
    private final Legend legend;
    private final UiChrome chrome;
    private final Paint bg=new Paint(),edge=new Paint(),button=new Paint(),
            title=new Paint(Paint.ANTI_ALIAS_FLAG),body=new Paint(Paint.ANTI_ALIAS_FLAG),
            option=new Paint(Paint.ANTI_ALIAS_FLAG),note=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String titleText="",bodyText="",noteText;
    private String[] options=new String[0];
    private int selected;
    private final List<String> titleLines=new ArrayList<String>(),bodyLines=new ArrayList<String>(),noteLines=new ArrayList<String>();
    public PromptView(Context c,AttributeSet a){
        super(c,a);d=UiScale.of(c);legend=new Legend(d);chrome=new UiChrome(d);
        bg.setColor(SonyUi.BACKGROUND);edge.setColor(SonyUi.RULE);edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(d);
        title.setColor(SonyUi.TEXT);title.setTextSize(SonyUi.TITLE*d);
        body.setColor(SonyUi.TEXT);body.setTextSize(SonyUi.BODY*d);
        option.setColor(SonyUi.TEXT);option.setTextSize(SonyUi.BODY*d);
        note.setColor(SonyUi.MUTED);note.setTextSize(SonyUi.SECONDARY*d);button.setColor(SonyUi.HEADER);
    }
    public void setTypeface(Typeface tf){
        title.setTypeface(tf);body.setTypeface(tf);option.setTypeface(tf);note.setTypeface(tf);legend.setTypeface(tf);
        requestLayout();invalidate();
    }
    public void set(String titleText,String bodyText,String[] options,int selected,String noteText){
        this.titleText=titleText;this.bodyText=bodyText;this.options=options;this.selected=selected;this.noteText=noteText;
        requestLayout();invalidate();
    }
    private void lines(float width){
        wrap(titleText,title,width,titleLines);wrap(bodyText,body,width,bodyLines);
        noteLines.clear();if(noteText!=null)wrap(noteText,note,width,noteLines);
    }
    private float contentHeight(){return (12+titleLines.size()*22+8+bodyLines.size()*18+12+28+12+noteLines.size()*16)*d;}
    @Override protected void onMeasure(int w,int h){
        float max=Math.max(1,MeasureSpec.getSize(w)-32*d),width=Math.min(SonyUi.DIALOG_WIDTH*d,max),pad=SonyUi.PAGE_PAD*d;
        lines(width-2*pad);
        float height=contentHeight()+legend.height(legend.lines(width-2*pad,Keys.hints(Keys.H_PROMPT,Keys.Caps.UNKNOWN)))+10*d;
        if(height>MeasureSpec.getSize(h)-24*d&&width<max){
            width=max;lines(width-2*pad);
            height=contentHeight()+legend.height(legend.lines(width-2*pad,Keys.hints(Keys.H_PROMPT,Keys.Caps.UNKNOWN)))+10*d;
        }
        setMeasuredDimension((int)Math.ceil(width),(int)Math.ceil(height));
    }
    private static void wrap(String text,Paint p,float width,List<String> out){
        out.clear();
        for(String paragraph:text.split("\n",-1)){
            int paragraphStart=out.size();
            String line="";
            for(String word:paragraph.split(" ")){
                if(word.isEmpty())continue;
                String next=line.isEmpty()?word:line+" "+word;
                if(p.measureText(next)<=width){line=next;continue;}
                if(!line.isEmpty())out.add(line);
                while(p.measureText(word)>width){
                    int n=SonyUi.safeEnd(word,p.breakText(word,true,width,null));
                    // Do not strand CJK closing punctuation at the start of a wrapped line.
                    while(n>0&&n<word.length()&&closing(word.charAt(n)))n=SonyUi.safeEnd(word,n-1);
                    while(n>0&&opening(word.charAt(n-1)))n=SonyUi.safeEnd(word,n-1);
                    if(n==0)n=Character.charCount(word.codePointAt(0));
                    out.add(word.substring(0,n));word=word.substring(n);
                }
                line=word;
            }
            if(!line.isEmpty())out.add(line);
            // Avoid a two-character CJK tail such as "变。"; prefer a preceding clause boundary.
            if(out.size()>paragraphStart+1){
                int last=out.size()-1;String tail=out.get(last),previous=out.get(last-1);
                if(tail.codePointCount(0,tail.length())<=3&&hasHan(tail)){
                    int cut=-1;for(int i=0;i<previous.length();i++)if("。！？；，".indexOf(previous.charAt(i))>=0)cut=i+1;
                    if(cut<=0||cut>=previous.length())cut=SonyUi.safeEnd(previous,Math.max(1,previous.length()-4));
                    while(cut>0&&cut<previous.length()&&closing(previous.charAt(cut)))cut=SonyUi.safeEnd(previous,cut-1);
                    String joined=previous.substring(cut)+tail;
                    if(cut>0&&p.measureText(joined)<=width){out.set(last-1,previous.substring(0,cut));out.set(last,joined);}
                }
            }
        }
    }
    private static boolean hasHan(String text){for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c>=0x3400&&c<=0x9fff)return true;}return false;}
    private static boolean closing(char c){return ",.;:!?，。；：！？、）】》」』〉〕’”".indexOf(c)>=0;}
    private static boolean opening(char c){return "（【《「『〈〔‘“".indexOf(c)>=0;}
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight(),pad=SonyUi.PAGE_PAD*d;
        c.drawRect(0,0,w,h,bg);c.drawRect(0,0,w,h,edge);
        float top=12*d;
        for(String s:titleLines){c.drawText(s,pad,CanvasText.baseline(title,top+11*d),title);top+=22*d;}
        top+=8*d;
        for(String s:bodyLines){c.drawText(s,pad,CanvasText.baseline(body,top+9*d),body);top+=18*d;}
        top+=12*d;
        float gap=SonyUi.ITEM_GAP*d,cell=(w-2*pad-Math.max(0,options.length-1)*gap)/Math.max(1,options.length);
        for(int i=0;i<options.length;i++){
            float left=pad+i*(cell+gap);
            if(i==selected)chrome.selection(c,left,top,left+cell,top+28*d,true);else c.drawRect(left,top,left+cell,top+28*d,button);
            String label=CanvasText.fit(option,options[i],cell-12*d);
            c.drawText(label,left+(cell-option.measureText(label))/2,CanvasText.baseline(option,top+14*d),option);
        }
        top+=40*d;
        for(String s:noteLines){c.drawText(s,pad,CanvasText.baseline(note,top+8*d),note);top+=16*d;}
        legend.drawWrapped(c,pad,top,w-2*pad,Keys.hints(Keys.H_PROMPT,Keys.Caps.UNKNOWN));
    }
}
