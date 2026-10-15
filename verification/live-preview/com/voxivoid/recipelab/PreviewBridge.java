package com.voxivoid.recipelab;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.view.View;
import com.sun.net.httpserver.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Developer-only UI harness. Production Canvas views; simulated Activity/camera/photo state. */
public final class PreviewBridge {
    private final Library library=new Library();
    private final ReviewLogic review=new ReviewLogic();
    private final java.util.List<Integer> favourites=new ArrayList<Integer>();
    private int recipe,row=Params.R_WBMODE,column=Params.COL_RECIPES,group,menuSel,lang=1,photo;
    private int[] stored=new int[Params.N],edit=new int[Params.N];
    private String page="home",reviewState="photo";
    private int panX,panY;
    private boolean hidden;
    private NameEntry nameEntry=NameEntry.blank("New recipe");
    private PreviewBridge(){
        stored[Params.R_QUAL]=Params.Q_FINE;stored[Params.R_KELVIN]=55;
        Params.stage(Recipes.ALL[0],stored);edit=stored.clone();
        for(int i=0;i<Recipes.ALL.length;i++)if(Recipes.ALL[i].name.equals("Provia")){recipe=i;break;}
        stage();group=library.get(recipe).group;
    }
    private void stage(){edit=stored.clone();Params.stage(library.get(recipe),edit);}
    private boolean active(){return Params.sameLook(stored,edit);}
    private Typeface face()throws Exception{return lang==0?Typeface.DEFAULT:Typeface.from("assets/fonts/RecipeLabCJK"+(lang==1?"sc":"tc")+"-Regular.ttf");}
    private static String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","");}
    private String snapshot(){
        return page+"|"+recipe+"|"+row+"|"+column+"|"+group+"|"+menuSel+"|"+lang+"|"+photo+"|"+review.level()+"|"+Params.rowsText(stored)+"|"+Params.rowsText(edit)+"|"+reviewState;
    }
    private void restore(String snapshot){try{
        String[] p=snapshot.split("\\|",-1);if(p.length!=12)return;
        if(!Arrays.asList("home","browser","parameter","menu","review","reset","about","exited","keyboard").contains(p[0]))return;
        int r=Integer.parseInt(p[1]);if(!library.valid(r))return;
        int[] a=Params.rowsFrom(p[9]),b=Params.rowsFrom(p[10]);if(a==null||b==null)return;
        page=p[0];recipe=r;row=Math.max(1,Math.min(Params.N-1,Integer.parseInt(p[2])));
        column=Integer.parseInt(p[3])==0?0:1;group=library.get(recipe).group;
        menuSel=Math.max(0,Math.min(DevTools.APP_ROWS-1,Integer.parseInt(p[5])));
        lang=Math.max(0,Math.min(2,Integer.parseInt(p[6])));photo=Math.max(0,Integer.parseInt(p[7]))%3;
        review.fit();for(int i=0;i<Math.min(3,Integer.parseInt(p[8]));i++)review.changeZoom(1);
        stored=a;edit=b;if(Arrays.asList("photo","loading","empty","error").contains(p[11]))reviewState=p[11];
    }catch(RuntimeException ignored){}}
    private String state(){Lang.use(lang);return "{\"page\":\""+page+"\",\"recipe\":\""+escape(Recipes.displayName(library.get(recipe)))+"\",\"active\":"+active()+",\"zoom\":"+review.zoom()+",\"lang\":"+lang+",\"snapshot\":\""+escape(snapshot())+"\"}";}
    private void command(String key){
        if(key.startsWith("page:")){page=key.substring(5);if(!Arrays.asList("home","browser","parameter","menu","review","keyboard","reset").contains(page))page="home";if(page.equals("reset"))menuSel=1;return;}
        if(key.startsWith("lang:")){lang=Math.max(0,Math.min(2,Integer.parseInt(key.substring(5))));return;}
        if(key.startsWith("review:")){reviewState=key.substring(7);return;}
        if(key.equals("menu-hold")){page="menu";return;}
        if(key.equals("enter-hold")){if(favourites.contains(recipe))favourites.remove(Integer.valueOf(recipe));else favourites.add(recipe);return;}
        int scan=key.equals("up")?Keys.K_UP:key.equals("down")?Keys.K_DOWN:key.equals("left")?Keys.K_LEFT:key.equals("right")?Keys.K_RIGHT:
            key.equals("enter")?Keys.K_ENTER:key.equals("menu")?Keys.K_MENU:key.equals("play")?Keys.K_PLAY:key.equals("fn")?Keys.K_FN:
            key.equals("wheel+")?Keys.K_WHEEL_CW:key.equals("wheel-")?Keys.K_WHEEL_CCW:key.equals("zoom+")?Keys.K_ZOOM_T:
            key.equals("zoom-")?Keys.K_ZOOM_W:key.equals("shutter")?Keys.K_S1:key.equals("trash")?Keys.K_DELETE:0;
        if(page.equals("keyboard")){
            if(scan==Keys.K_MENU){page="menu";return;}
            if(scan==Keys.K_LEFT||scan==Keys.K_RIGHT)nameEntry.move(0,scan==Keys.K_RIGHT?1:-1);
            if(scan==Keys.K_UP||scan==Keys.K_DOWN)nameEntry.move(scan==Keys.K_DOWN?1:-1,0);
            if(Keys.turn(scan)!=0)nameEntry.step(Keys.turn(scan));
            if(scan==Keys.K_DELETE)nameEntry.backspace();
            if(scan==Keys.K_ENTER&&nameEntry.press()==NameEntry.DONE)page="menu";
            return;
        }
        if(page.equals("review")){
            int action=ReviewLogic.command(scan,false,review.zoomed());
            if(action==ReviewLogic.CLOSE){page="home";return;}
            if(action==ReviewLogic.NEXT||action==ReviewLogic.PREVIOUS){photo=(photo+3+(action==ReviewLogic.NEXT?1:-1))%3;review.fit();panX=panY=0;}
            if(action==ReviewLogic.ZOOM_IN||action==ReviewLogic.ZOOM_OUT)review.changeZoom(action==ReviewLogic.ZOOM_IN?1:-1);
            if(action==ReviewLogic.FIT){review.fit();panX=panY=0;}
            if(action==ReviewLogic.PAN_LEFT)panX+=32;if(action==ReviewLogic.PAN_RIGHT)panX-=32;
            if(action==ReviewLogic.PAN_UP)panY+=32;if(action==ReviewLogic.PAN_DOWN)panY-=32;return;
        }
        if(scan==Keys.K_PLAY){page="review";review.fit();panX=panY=0;return;}
        if(scan==Keys.K_FN){page=page.equals("browser")?"home":"browser";group=library.get(recipe).group;return;}
        if(page.equals("reset")){if(scan==Keys.K_LEFT||scan==Keys.K_RIGHT)menuSel=1-menuSel;if(scan==Keys.K_MENU){page="menu";return;}if(scan==Keys.K_ENTER){if(menuSel==0){recipe=0;stage();stored=edit.clone();}page="home";}return;}
        if(page.equals("about")){if(scan==Keys.K_MENU)page="menu";return;}
        if(page.equals("menu")){
            int direction=scan==Keys.K_UP?-1:scan==Keys.K_DOWN?1:Keys.turn(scan);
            if(direction!=0)menuSel=DevTools.nextRow(DevTools.LEVEL_APP,menuSel,direction);
            if(scan==Keys.K_MENU)page="home";
            if(scan==Keys.K_ENTER){
                if(menuSel==DevTools.APP_BROWSE)page="browser";else if(menuSel==DevTools.APP_REVIEW)page="review";
                else if(menuSel==DevTools.APP_NEW)page="keyboard";
                else if(menuSel==DevTools.APP_LANG)lang=(lang+1)%3;else if(menuSel==DevTools.APP_RESET){page="reset";menuSel=1;}
                else if(menuSel==DevTools.APP_ABOUT)page="about";
            }return;
        }
        if(page.equals("browser")){
            int direction=scan==Keys.K_UP?-1:scan==Keys.K_DOWN?1:Keys.turn(scan);
            if(scan==Keys.K_LEFT||scan==Keys.K_RIGHT)column=1-column;
            if(direction!=0){
                if(column==Params.COL_GROUPS){group=Favourites.nextGroup(group,direction);int next=Favourites.landing(group,favourites,library);if(next>=0){recipe=next;stage();}}
                else if(Favourites.hasRecipes(group,favourites,library)){recipe=group==Favourites.GROUP?Favourites.next(favourites,recipe,direction):Recipes.nextInGroup(recipe,direction);stage();}
            }
            if(scan==Keys.K_ENTER){if(column==Params.COL_GROUPS)column=Params.COL_RECIPES;else {stored=edit.clone();page="home";}}
            if(scan==Keys.K_MENU)page="home";return;
        }
        if(page.equals("parameter")){
            int direction=scan==Keys.K_LEFT?-1:scan==Keys.K_RIGHT?1:Keys.turn(scan);
            if(direction!=0)Params.step(edit,row,direction,Params.Q_FINE);
            if(scan==Keys.K_UP||scan==Keys.K_DOWN)row=Params.nextChip(row,scan==Keys.K_DOWN?1:-1,edit);
            if(scan==Keys.K_ENTER||scan==Keys.K_MENU)page="home";return;
        }
        int direction=scan==Keys.K_LEFT?-1:scan==Keys.K_RIGHT?1:Keys.turn(scan);
        if(direction!=0){recipe=library.next(recipe,direction);stage();}
        if(scan==Keys.K_ENTER){stored=edit.clone();page="home";hidden=false;}
        if(scan==Keys.K_UP||scan==Keys.K_DOWN)page="parameter";
        if(scan==Keys.K_MENU)page="exited";
        if(scan==Keys.K_DELETE)hidden=!hidden;
    }
    private BufferedImage scene(boolean photo){
        BufferedImage img=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();
        // Developer placeholder only. No test chart that could be mistaken for an app panel.
        g.setPaint(new GradientPaint(0,0,new Color(49,51,53),640,480,new Color(25,27,29)));
        g.fillRect(0,0,640,480);
        String label=photo?"PHOTO "+String.format("%02d",this.photo+1)+" / PLACEHOLDER":"LIVE VIEW / PLACEHOLDER";
        g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(new Color(116,119,122));
        g.drawString(label,(640-g.getFontMetrics().stringWidth(label))/2,216);
        g.dispose();return img;
    }
    private void draw(BufferedImage frame,View view,int x,int y)throws Exception{
        Canvas canvas=new Canvas(view.getWidth(),view.getHeight());view.draw(canvas);
        if(!canvas.overflow.isEmpty())throw new IllegalStateException("Text bounds: "+canvas.overflow);
        Graphics2D g=frame.createGraphics();g.drawImage(canvas.image,x,y,null);g.dispose();
    }
    private byte[] render()throws Exception{
        Lang.use(lang);Typeface face=face();Context context=new Context();
        BufferedImage frame=scene(false);
        if(page.equals("browser")){
            PickerView v=new PickerView(context,null);v.setTypeface(face);v.setCaps(new Keys.Caps(true));v.set(recipe,column,group,favourites,library,true,active());draw(frame,v,0,0);
        }else if(page.equals("menu")||page.equals("about")){
            MenuView v=new MenuView(context,null);v.setTypeface(face);
            if(page.equals("about"))v.setPage(DevTools.aboutTitle(),new String[][]{{"Version","1.6.2-rx100m5.7-test"}},Keys.hints(Keys.H_PAGE,Keys.Caps.UNKNOWN));
            else {int n=DevTools.APP_ROWS;String[] labels=new String[n],details=new String[n],values=new String[n];for(int i=0;i<n;i++){labels[i]=DevTools.appLabel(i);details[i]=DevTools.appDetail(i);values[i]=DevTools.appValue(i,Params.OV_FULL,lang);}v.set(Lang.t("menu_title"),labels,details,values,null,DevTools.icons(0),menuSel,Keys.hints(Keys.H_MENU_TOP,new Keys.Caps(true)));}draw(frame,v,0,0);
        }else if(page.equals("review")){
            Graphics2D g=frame.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,0,640,480);
            int imageHeight=480-PhotoReviewChrome.HEIGHT*2;
            if(reviewState.equals("photo")){BufferedImage photoImage=scene(true);float z=review.zoom();int w=(int)(imageHeight*4f/3*z),h=(int)(imageHeight*z);panX=Math.max(-Math.max(0,(w-640)/2),Math.min(Math.max(0,(w-640)/2),panX));panY=Math.max(-(h-imageHeight)/2,Math.min((h-imageHeight)/2,panY));g.setClip(0,0,640,imageHeight);g.drawImage(photoImage,(640-w)/2+panX,(imageHeight-h)/2+panY,w,h,null);}g.dispose();
            PhotoReviewChrome v=new PhotoReviewChrome(context,null);v.setTypeface(face);String text=reviewState.equals("photo")?String.format("DSC%05d.JPG",photo+1):Lang.t(reviewState.equals("loading")?"review_loading":reviewState.equals("empty")?"review_empty":"review_unavailable");v.set(text,review.zoom());v.measure(640,480);draw(frame,v,0,480-v.getHeight());
        }else if(page.equals("parameter")){
            if(row==Params.R_KELVIN&&edit[Params.R_WBMODE]!=Params.WB_KELVIN)row=Params.R_WBMODE;
            ParameterCardView v=new ParameterCardView(context,null);v.setTypeface(face);v.set(Params.rowName(row),Params.fmt(row,edit[row],edit));v.measure(640,480);draw(frame,v,(640-v.getWidth())/2,(480-v.getHeight())/2);
        }else if(page.equals("reset")){
            PromptView v=new PromptView(context,null);v.setTypeface(face);v.set(DevTools.resetTitle(),DevTools.resetBody(),DevTools.resetOptions(),menuSel,null);v.measure(640,480);draw(frame,v,(640-v.getWidth())/2,(480-v.getHeight())/2);
        }else if(page.equals("keyboard")){
            KeyboardView v=new KeyboardView(context,null);v.setTypeface(face);v.set(Lang.t("menu_new"),nameEntry,Keys.hints(Keys.H_NAME,Keys.Caps.UNKNOWN));draw(frame,v,0,0);
        }else if(!hidden&&!page.equals("exited")){
            Recipes.Recipe selected=library.get(recipe);
            LiveRecipeView v=new LiveRecipeView(context,null);v.setTypeface(face);
            v.set(Recipes.displayName(selected),Recipes.groupLabel(selected.group),library.position(recipe),LiveRecipeView.summary(edit),active(),Params.differsFromRecipe(selected,edit),new Keys.Caps(true),true);
            v.measure(640,480);draw(frame,v,0,480-v.getHeight());
        }
        ByteArrayOutputStream out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(frame,"png",out);return out.toByteArray();
    }
    private static Map<String,String> form(String data)throws Exception{
        Map<String,String> out=new HashMap<String,String>();for(String pair:data.split("&")){String[] p=pair.split("=",2);if(p.length==2)out.put(URLDecoder.decode(p[0],"UTF-8"),URLDecoder.decode(p[1],"UTF-8"));}return out;
    }
    public static void main(String[] args)throws Exception{
        final PreviewBridge bridge=new PreviewBridge();HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",new HttpHandler(){public void handle(HttpExchange exchange)throws IOException{
            synchronized(bridge){try{
                String path=exchange.getRequestURI().getPath();byte[] result;
                if(path.equals("/frame.png")){result=bridge.render();exchange.getResponseHeaders().set("Content-Type","image/png");}
                else {
                    if(path.equals("/command")||path.equals("/restore")){
                        if(!exchange.getRequestMethod().equals("POST")){exchange.sendResponseHeaders(405,-1);return;}
                        byte[] data=readLimited(exchange.getRequestBody(),8192);Map<String,String> form=form(new String(data,StandardCharsets.UTF_8));
                        if(path.equals("/restore"))bridge.restore(form.get("snapshot"));else {String key=form.get("key");if(key==null||key.length()>32){exchange.sendResponseHeaders(400,-1);return;}bridge.command(key);}
                    }
                    result=bridge.state().getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
                }
                exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.sendResponseHeaders(200,result.length);exchange.getResponseBody().write(result);
            }catch(Exception failure){byte[] text="Preview failed; inspect local developer log".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(500,text.length);exchange.getResponseBody().write(text);failure.printStackTrace();}finally{exchange.close();}}
        }});server.start();System.out.println("READY_PORT="+server.getAddress().getPort());
    }
    private static byte[] readLimited(InputStream input,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[1024];for(int n;(n=input.read(block))>=0;){if(out.size()+n>limit)throw new IOException("Too large");out.write(block,0,n);}return out.toByteArray();}
}
