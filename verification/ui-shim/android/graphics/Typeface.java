package android.graphics;
public class Typeface {
    public final java.awt.Font font;
    public static final Typeface DEFAULT = new Typeface(new java.awt.Font("SansSerif",0,12));
    public Typeface(java.awt.Font font) { this.font=font; }
    public static Typeface from(String path) throws Exception { return new Typeface(java.awt.Font.createFont(0,new java.io.File(path))); }
}
