package android.view;
public class View {
    private int w=640,h=480;
    public View(android.content.Context c, android.util.AttributeSet a) {}
    public void invalidate() {} public void requestLayout() {}
    public int getWidth() { return w; } public int getHeight() { return h; }
    public void setMeasuredDimension(int width,int height) { w=width; h=height; }
    protected void onMeasure(int w,int h) { setMeasuredDimension(MeasureSpec.getSize(w),MeasureSpec.getSize(h)); }
    protected void onDraw(android.graphics.Canvas c) {}
    public void measure(int w,int h) { onMeasure(w,h); }
    public void draw(android.graphics.Canvas c) { onDraw(c); }
    public static class MeasureSpec { public static int getSize(int n) { return n & 0x3fffffff; } }
}
