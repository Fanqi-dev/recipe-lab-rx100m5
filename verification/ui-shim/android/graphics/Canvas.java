package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
public class Canvas {
    public final BufferedImage image;
    private final Graphics2D g;
    public final java.util.List<String> overflow=new java.util.ArrayList<String>();
    public final java.util.List<TextRun> texts=new java.util.ArrayList<TextRun>();
    public final java.util.List<RectRun> rects=new java.util.ArrayList<RectRun>();
    public static final class RectRun {
        public final float left,top,right,bottom;public final boolean round;
        RectRun(float l,float t,float r,float b,boolean round){left=l;top=t;right=r;bottom=b;this.round=round;}
    }
    public static final class TextRun {
        public final String text; public final float x,y,width,centerY,top,bottom,size;
        public final boolean bold;
        TextRun(String s,float x,float y,Paint p){text=s;this.x=x;this.y=y;width=p.measureText(s);centerY=y+(p.ascent()+p.descent())/2;top=y+p.ascent();bottom=y+p.descent();size=p.getTextSize();bold=p.bold;}
    }
    public Canvas(){this(2,2);} public Canvas(int w,int h){image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB); g=image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
    private void paint(Shape shape,Paint p){g.setColor(new Color(p.color,true)); g.setStroke(new BasicStroke(Math.max(.1f,p.width),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); if(p.style!=Paint.Style.STROKE)g.fill(shape);if(p.style!=Paint.Style.FILL)g.draw(shape);}
    public void drawRect(float l,float t,float r,float b,Paint p){rects.add(new RectRun(l,t,r,b,false));paint(new Rectangle2D.Float(l,t,r-l,b-t),p);}
    public void drawRect(RectF r,Paint p){drawRect(r.left,r.top,r.right,r.bottom,p);}
    public void drawRoundRect(RectF r,float rx,float ry,Paint p){rects.add(new RectRun(r.left,r.top,r.right,r.bottom,true));paint(new RoundRectangle2D.Float(r.left,r.top,r.right-r.left,r.bottom-r.top,rx*2,ry*2),p);}
    public void drawOval(RectF r,Paint p){paint(new Ellipse2D.Float(r.left,r.top,r.right-r.left,r.bottom-r.top),p);}
    public void drawCircle(float x,float y,float r,Paint p){paint(new Ellipse2D.Float(x-r,y-r,r*2,r*2),p);}
    public void drawLine(float x,float y,float a,float b,Paint p){g.setColor(new Color(p.color,true));g.setStroke(new BasicStroke(Math.max(.1f,p.width)));g.draw(new Line2D.Float(x,y,a,b));}
    public void drawArc(RectF r,float start,float sweep,boolean center,Paint p){paint(new Arc2D.Float(r.left,r.top,r.right-r.left,r.bottom-r.top,-start,-sweep,center?Arc2D.PIE:Arc2D.OPEN),p);}
    public void drawPath(Path path,Paint p){paint(path.path,p);}
    public void drawText(String text,float x,float y,Paint p){float w=p.measureText(text);if(p.align==Paint.Align.CENTER)x-=w/2;if(p.align==Paint.Align.RIGHT)x-=w; texts.add(new TextRun(text,x,y,p));if(image.getWidth()>2&&(x<-.5||x+w>image.getWidth()+.5||y+p.ascent()<-.5||y+p.descent()>image.getHeight()+.5))overflow.add(text);g.setFont(p.font());g.setColor(new Color(p.color,true));g.drawString(text,x,y);}
    public void save(String path) throws Exception {javax.imageio.ImageIO.write(image,"png",new java.io.File(path));}
}
