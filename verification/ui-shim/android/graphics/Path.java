package android.graphics;
public class Path {
    final java.awt.geom.Path2D.Float path=new java.awt.geom.Path2D.Float();
    public void reset(){path.reset();} public void moveTo(float x,float y){path.moveTo(x,y);}
    public void lineTo(float x,float y){path.lineTo(x,y);} public void close(){path.closePath();}
    public void cubicTo(float a,float b,float c,float d,float e,float f){path.curveTo(a,b,c,d,e,f);}
}
