package com.voxivoid.recipelab;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Same-Activity native read-only review. Never uses BitmapFactory or scans DCIM. */
final class SonyPhotoReview extends FrameLayout {
    interface Listener{void closed(boolean released);}
    private final Listener listener;
    private final Handler ui=new Handler();
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
    private final ReviewLogic model=new ReviewLogic();
    private final ReviewLogic.Tickets tickets=new ReviewLogic.Tickets();
    private final PhotoReviewChrome footer;
    private SonyPhotoSource source;
    private View display;
    private Object image,bound,positionType,displayType;
    private Method setImage,setScale,setTranslate,translate,releaseView,setDisplayType;
    private Method setDisplayListener;
    private Class<?> displayListenerType;
    private int displayOk;
    private volatile boolean open,closing;
    private volatile boolean releaseFailed;
    private boolean mainImage,disposed;
    private String filename="";
    SonyPhotoReview(Context context,Listener listener){
        super(context);this.listener=listener;
        setBackgroundColor(0xFF000000);setVisibility(GONE);
        footer=new PhotoReviewChrome(context,null);
        LayoutParams foot=new LayoutParams(-1,(int)Math.ceil(PhotoReviewChrome.HEIGHT*UiScale.of(context)),Gravity.BOTTOM);
        addView(footer,foot);
    }
    void setTypeface(Typeface face){footer.setTypeface(face);}
    boolean isOpen(){return open||closing;}
    /** Probe the actual device API before releasing the shooting camera. No bundled Sony implementation. */
    void prepare() throws Exception {
        if(disposed)throw new IllegalStateException("Disposed");
        Class<?> view=Class.forName("com.sony.scalar.widget.OptimizedImageView");
        Class<?> optimized=Class.forName("com.sony.scalar.graphics.OptimizedImage");
        Class<?> bounds=Class.forName("com.sony.scalar.widget.OptimizedImageView$BoundType");
        Class<?> positions=Class.forName("com.sony.scalar.widget.OptimizedImageView$TranslationType");
        Class<?> types=Class.forName("com.sony.scalar.widget.OptimizedImageView$DisplayType");
        bound=bounds.getField("BOUND_TYPE_LONG_EDGE").get(null);
        positionType=positions.getField("TRANS_TYPE_INNER_CENTER").get(null);
        displayType=types.getField("DISPLAY_TYPE_CENTER_INNER").get(null);
        setImage=view.getMethod("setOptimizedImage",optimized);setScale=view.getMethod("setScale",float.class,bounds);
        setTranslate=view.getMethod("setTranslate",Point.class,positions);translate=view.getMethod("translate",Point.class);
        releaseView=view.getMethod("release");setDisplayType=view.getMethod("setDisplayType",types);
        optimized.getMethod("release");
        displayListenerType=Class.forName("com.sony.scalar.widget.OptimizedImageView$onDisplayEventListener");
        setDisplayListener=view.getMethod("setOnDisplayEventListener",displayListenerType);
        displayOk=Class.forName("com.sony.scalar.widget.OptimizedImageView$ErrCd").getField("ER_OK").getInt(null);
        // Constructing the SurfaceView happens only AFTER capture is released in open().
        view.getConstructor(Context.class);
        source=new SonyPhotoSource(getContext());
    }
    void open(){
        if(isOpen()||disposed)return;
        open=true;model.fit();mainImage=false;filename="";
        tickets.begin();setVisibility(VISIBLE);
        try{
            display=(View)Class.forName("com.sony.scalar.widget.OptimizedImageView").getConstructor(Context.class).newInstance(getContext());
            LayoutParams photo=new LayoutParams(-1,-1);photo.bottomMargin=(int)Math.ceil(PhotoReviewChrome.HEIGHT*UiScale.of(getContext()));
            addView(display,0,photo);setDisplayType.invoke(display,displayType);
            request(1,false);
        }catch(Throwable failure){footer.set(Lang.t("review_unavailable"),1);}
    }
    private boolean detach(){
        if(display!=null)try{setImage.invoke(display,new Object[]{null});}catch(Exception failure){return false;}
        if(!SonyPhotoSource.release(image)){releaseFailed=true;return false;}
        image=null;mainImage=false;return true;
    }
    private void request(final int direction,final boolean main){
        if(!open||closing)return;
        final int ticket=tickets.next();
        if(!detach()){footer.set(Lang.t("review_unavailable"),model.zoom());return;}
        footer.set(Lang.t("review_loading"),model.zoom());
        worker.getQueue().clear();
        worker.execute(new Runnable(){public void run(){
            Object decoded=null;
            try{
                String path=source.select(direction,tickets,ticket);
                if(!tickets.accepts(ticket))return;
                if(path==null){postError(ticket,"review_empty");return;}
                decoded=source.decode(path,main);
                if(decoded==null)throw new IllegalStateException("DECODE_FAILED");
                if(!tickets.accepts(ticket)){if(!SonyPhotoSource.release(decoded))releaseFailed=true;return;}
                final Object result=decoded;final String name=new File(path).getName();
                ui.post(new Runnable(){public void run(){
                    if(!tickets.accepts(ticket)){if(!SonyPhotoSource.release(result))releaseFailed=true;return;}
                    image=result; // Own the handle before any reflective operation can fail.
                    try{
                        registerDisplayListener(ticket);
                        setImage.invoke(display,result);mainImage=main;filename=name;
                        viewport(true);footer.set(filename,model.zoom());
                    }catch(Throwable failure){detach();footer.set(Lang.t("review_unavailable"),1);}
                }});
            }catch(Throwable failure){if(!SonyPhotoSource.release(decoded))releaseFailed=true;postError(ticket,"review_unavailable");}
        }});
    }
    private void postError(final int ticket,final String key){ui.post(new Runnable(){public void run(){
        if(tickets.accepts(ticket))footer.set(Lang.t(key),1);
    }});}
    private void registerDisplayListener(final int ticket)throws Exception{
        Object callback=Proxy.newProxyInstance(displayListenerType.getClassLoader(),new Class<?>[]{displayListenerType},new InvocationHandler(){
            public Object invoke(Object proxy,Method method,Object[] args){
                if(method.getName().equals("onDisplay")&&args!=null&&args.length==1){
                    if(((Integer)args[0]).intValue()!=displayOk)postError(ticket,"review_unavailable");return null;
                }
                if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
                if(method.getName().equals("equals"))return args!=null&&args.length==1&&proxy==args[0];
                if(method.getName().equals("toString"))return "RecipeLabReviewDisplayListener";
                return null;
            }
        });
        setDisplayListener.invoke(display,callback);
    }
    private void viewport(boolean center) throws Exception{
        if(display==null||image==null)return;
        setScale.invoke(display,model.zoom(),bound);
        if(center)setTranslate.invoke(display,new Point(Math.max(1,display.getWidth())/2,Math.max(1,display.getHeight())/2),positionType);
    }
    void command(int command){
        if(closing)return;
        if(command==ReviewLogic.CLOSE){close();return;}
        if(command==ReviewLogic.PREVIOUS||command==ReviewLogic.NEXT){model.fit();request(command==ReviewLogic.NEXT?1:-1,false);return;}
        if(image==null)return;
        try{
            if(command==ReviewLogic.FIT){model.fit();viewport(true);footer.set(filename,1);return;}
            if(command==ReviewLogic.ZOOM_IN||command==ReviewLogic.ZOOM_OUT){
                if(!model.changeZoom(command==ReviewLogic.ZOOM_IN?1:-1))return;
                if(model.zoomed()&&!mainImage){request(0,true);return;}
                viewport(!model.zoomed());footer.set(filename,model.zoom());return;
            }
            int delta=(int)Math.ceil(16*UiScale.of(getContext()));
            int x=command==ReviewLogic.PAN_LEFT?delta:command==ReviewLogic.PAN_RIGHT?-delta:0;
            int y=command==ReviewLogic.PAN_UP?delta:command==ReviewLogic.PAN_DOWN?-delta:0;
            if(model.zoomed()&&(x!=0||y!=0))translate.invoke(display,new Point(x,y));
        }catch(Throwable failure){footer.set(Lang.t("review_unavailable"),model.zoom());}
    }
    boolean zoomed(){return model.zoomed();}
    void close(){
        if(closing||!open)return;
        closing=true;open=false;tickets.close();detach();
        footer.set(Lang.t("review_returning"),1);
        worker.getQueue().clear();
        // Queue behind the current native decode. Do NOT reopen the camera while it still owns decoder resources.
        worker.execute(new Runnable(){public void run(){
            if(source!=null)try{source.close();}catch(Exception failure){releaseFailed=true;}
            ui.post(new Runnable(){public void run(){
                if(display!=null){try{releaseView.invoke(display);}catch(Exception failure){releaseFailed=true;}removeView(display);display=null;}
                if(!SonyPhotoSource.release(image))releaseFailed=true;image=null;
                closing=false;setVisibility(GONE);listener.closed(!releaseFailed);
                if(disposed)worker.shutdown();
            }});
        }});
    }
    void dispose(){disposed=true;if(isOpen())close();else worker.shutdown();}
}
