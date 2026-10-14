package android.graphics;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
public class Paint {
    public static final int ANTI_ALIAS_FLAG=1;
    public enum Style { FILL, STROKE, FILL_AND_STROKE } public enum Align { LEFT,CENTER,RIGHT } public enum Cap { ROUND }
    int color=0xff000000; float size=12,width=1; boolean bold=false;
    Style style=Style.FILL; Align align=Align.LEFT; Typeface typeface=Typeface.DEFAULT;
    static final FontRenderContext FRC=new FontRenderContext(new AffineTransform(),true,true);
    public Paint(){} public Paint(int flags){}
    public void setColor(int n){color=n;} public void setTextSize(float n){size=n;} public float getTextSize(){return size;}
    public void setFakeBoldText(boolean n){bold=n;} public void setTypeface(Typeface n){typeface=n==null?Typeface.DEFAULT:n;}
    public void setStyle(Style n){style=n;} public void setStrokeWidth(float n){width=n;} public void setStrokeCap(Cap n){}
    public void setTextAlign(Align n){align=n;}
    java.awt.Font font(){return typeface.font.deriveFont(bold?1:0,size);}
    public float measureText(String s){return (float)font().getStringBounds(s,FRC).getWidth();}
    public int breakText(String s,boolean forward,float max,float[] measured){int n=0; while(n<s.length()&&measureText(s.substring(0,n+1))<=max)n++;return n;}
    public float ascent(){return -font().getLineMetrics("Ag",FRC).getAscent();} public float descent(){return font().getLineMetrics("Ag",FRC).getDescent();}
}
