package com.voxivoid.recipelab;

import java.util.Locale;

/** Shared, hardware-free review controls. No photo writes, delete, reset or recipe application. */
final class ReviewLogic {
    static final int NONE=0,CLOSE=1,PREVIOUS=2,NEXT=3,ZOOM_IN=4,ZOOM_OUT=5,FIT=6,
            PAN_LEFT=7,PAN_RIGHT=8,PAN_UP=9,PAN_DOWN=10;
    private int level;
    int level() { return level; }
    float zoom() { return 1 << level; }
    boolean zoomed() { return level>0; }
    void fit() { level=0; }
    boolean changeZoom(int direction) {
        int next=Math.max(0,Math.min(3,level+(direction>0?1:-1)));
        boolean changed=next!=level; level=next; return changed;
    }
    static boolean jpeg(String path) {
        if(path==null || !path.startsWith("/") || path.indexOf('\0')>=0) return false;
        String p=path.toLowerCase(Locale.US);
        return p.endsWith(".jpg") || p.endsWith(".jpeg");
    }
    interface Rows{String path(int position) throws Exception;}
    static int find(int count,int position,int direction,Rows rows,Tickets tickets,int ticket) throws Exception{
        if(count<=0)return -1;
        int step=direction<0?-1:1;
        int at=position<0?(step<0?count-1:0):position+step;
        for(int visited=0;visited<count;visited++,at+=step){
            if(!tickets.accepts(ticket))return -1;
            at=(at+count)%count;
            if(jpeg(rows.path(at)))return at;
        }
        return -1;
    }
    static int command(int scan,boolean back,boolean zoomed) {
        if(back || Keys.isMenu(scan) || scan==Keys.K_PLAY || scan==Keys.K_S1 || scan==517) return CLOSE;
        if(scan==Keys.K_ZOOM_T) return ZOOM_IN;
        if(scan==Keys.K_ZOOM_W) return ZOOM_OUT;
        if(scan==Keys.K_ENTER) return zoomed?FIT:ZOOM_IN;
        if(scan==Keys.K_LEFT) return zoomed?PAN_LEFT:PREVIOUS;
        if(scan==Keys.K_RIGHT) return zoomed?PAN_RIGHT:NEXT;
        if(scan==Keys.K_UP) return zoomed?PAN_UP:NONE;
        if(scan==Keys.K_DOWN) return zoomed?PAN_DOWN:NONE;
        int direction=Keys.turn(scan);
        if(direction!=0) return zoomed?(direction>0?ZOOM_IN:ZOOM_OUT):(direction>0?NEXT:PREVIOUS);
        return NONE;
    }
    /** Tags prevent an old decode from appearing after a new selection or closing the viewer. */
    static final class Tickets {
        private volatile int generation;
        private volatile boolean open;
        synchronized int begin() { open=true; return ++generation; }
        synchronized int next() { return ++generation; }
        synchronized void close() { open=false; generation++; }
        boolean accepts(int ticket) { return open && generation==ticket; }
    }
}
