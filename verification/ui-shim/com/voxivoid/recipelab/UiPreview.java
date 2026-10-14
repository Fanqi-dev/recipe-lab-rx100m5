package com.voxivoid.recipelab;
import android.content.Context;
import android.graphics.*;
import java.util.*;
/** Desktop-only geometry preview of production Canvas views, NOT an Android/camera runtime. */
public class UiPreview {
    static String out;
    static int checks;
    public static void main(String[] args) throws Exception {
        out=args[0]; new java.io.File(out).mkdirs();
        for(int lang=0;lang<3;lang++) {
            Lang.use(lang);
            Typeface face=lang==0?Typeface.DEFAULT:Typeface.from("assets/fonts/RecipeLabCJK"+(lang==1?"sc":"tc")+"-Regular.ttf");
            for(int selected=0;selected<DevTools.APP_ROWS;selected++) {
                MenuView v=new MenuView(new Context(),null);v.setTypeface(face);
                String[] labels=new String[DevTools.APP_ROWS],details=labels.clone(),values=labels.clone();
                for(int i=0;i<labels.length;i++){labels[i]=DevTools.appLabel(i);details[i]=DevTools.appDetail(i);values[i]=DevTools.appValue(i,0,lang);}
                int mode=values[selected]==null?Keys.H_MENU_TOP:Keys.H_MENU_TOP_VALUE;
                v.set(Lang.t("menu_title"),labels,details,values,null,DevTools.icons(0),selected,Keys.hints(mode,Keys.Caps.UNKNOWN));
                check(v,"menu-"+lang+"-"+selected);
            }
            Library lib=new Library();
            for(int g=0;g<Recipes.GROUPS.length;g++) {
                int index=Favourites.recipeAt(g,0,Collections.<Integer>emptyList(),lib);
                if(index<0)continue;
                PickerView v=new PickerView(new Context(),null);v.setTypeface(face);
                v.set(index,1,g,Arrays.asList(index),lib,true,false);check(v,"picker-"+lang+"-"+g);
                v.set(index,0,g,Arrays.asList(index),lib,true,true);check(v,"picker-applied-"+lang+"-"+g);
            }
            for (int selected=0;selected<DevTools.ROWS;selected++) {
                int n=DevTools.ROWS;
                String[] labels=new String[n],details=new String[n],values=new String[n];
                for(int i=0;i<n;i++){labels[i]=DevTools.rowLabel(i,false,1);details[i]=DevTools.rowDetail(i,false);values[i]=DevTools.rowValue(i,1);}
                MenuView v=new MenuView(new Context(),null);v.setTypeface(face);
                v.set(Lang.t("menu_title")+" / "+DevTools.title(),labels,details,values,null,DevTools.icons(1),selected,
                    Keys.hints(values[selected]==null?Keys.H_MENU_SUB:Keys.H_MENU_SUB_VALUE,Keys.Caps.UNKNOWN));
                check(v,"submenu-"+lang+"-"+selected);
            }
            for(boolean applied:new boolean[]{false,true}) for(boolean quiet:new boolean[]{false,true}) {
                int index=Favourites.recipeAt(1,0,Collections.<Integer>emptyList(),lib);
                Recipes.Recipe recipe=lib.get(index);
                LiveRecipeView live=new LiveRecipeView(new Context(),null);live.setTypeface(face);live.setCaps(new Keys.Caps(true));
                int[] model=new int[Params.ROW_NAME.length]; model[Params.R_WBMODE]=Params.WB_AUTO;
                Params.stage(recipe,model);
                String wb=model[Params.R_WBMODE]==Params.WB_KELVIN?Params.fmt(Params.R_KELVIN,model[Params.R_KELVIN],model):Params.fmt(Params.R_WBMODE,model[Params.R_WBMODE],model);
                String summary=Params.fmt(Params.R_STYLE,model[Params.R_STYLE],model)+"   WB "+wb+"   EV "+Params.fmt(Params.R_EV,model[Params.R_EV],model);
                live.set(Recipes.displayName(recipe),Recipes.groupLabel(recipe.group),lib.position(index),summary,applied,false,quiet);
                live.measure(640,480);
                if(live.getHeight()>208)throw new AssertionError("HUD consumes more than 44% of screen: "+live.getHeight());
                String name="live-"+lang+"-"+applied+"-"+quiet;
                check(live,name);
                // Neutral area represents the missing hardware SurfaceView; it is NOT a camera photo.
                java.awt.image.BufferedImage frame=new java.awt.image.BufferedImage(640,480,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g=frame.createGraphics();g.setColor(new java.awt.Color(32,32,32));g.fillRect(0,0,640,480);
                g.drawImage(javax.imageio.ImageIO.read(new java.io.File(out+"/"+name+".png")),0,480-live.getHeight(),null);g.dispose();
                javax.imageio.ImageIO.write(frame,"png",new java.io.File(out+"/"+name+"-frame.png"));
            }
            ParameterCardView card=new ParameterCardView(new Context(),null);card.setTypeface(face);
            card.set(Params.rowName(Params.R_KELVIN),"5500K");card.measure(640,480);check(card,"parameter-"+lang);
            PromptView p=new PromptView(new Context(),null);p.setTypeface(face);
            p.set(DevTools.resetTitle(),DevTools.resetBody(),DevTools.resetOptions(),DevTools.RESET_DEFAULT,null);
            p.measure(640,480);check(p,"reset-"+lang);
            KeyboardView k=new KeyboardView(new Context(),null);k.setTypeface(face);
            k.set(Lang.t("menu_new"),NameEntry.blank("New recipe"),Keys.hints(Keys.H_NAME,Keys.Caps.UNKNOWN));check(k,"keyboard-"+lang);
        }
        Paint paint=new Paint();paint.setTextSize(20);
        for(String s:new String[]{"short","A very long custom recipe filename that needs trimming", "照片配方菜单"})
            for(float width:new float[]{0,5,20,40,100,200})
                if(paint.measureText(CanvasText.fit(paint,s,width))>width+.1)throw new AssertionError("Text overflow");
        System.out.println(checks+" source-driven desktop layout previews passed bounds checks. Not camera screenshots.");
        overview();
    }
    static void check(android.view.View v,String name)throws Exception {
        Canvas c=new Canvas(v.getWidth(),v.getHeight());v.draw(c);
        if(!c.overflow.isEmpty())throw new AssertionError(name+" text outside viewport: "+c.overflow);
        if(v.getHeight()>480)throw new AssertionError(name+" too tall: "+v.getHeight());
        c.save(out+"/"+name+".png");checks++;
    }
    static void overview()throws Exception {
        java.awt.image.BufferedImage sheet=new java.awt.image.BufferedImage(1328,1080,java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g=sheet.createGraphics();
        g.setColor(new java.awt.Color(20,20,20));g.fillRect(0,0,1328,1080);
        g.setFont(new java.awt.Font("SansSerif",0,18));g.setColor(java.awt.Color.LIGHT_GRAY);
        g.drawString("Recipe Lab / Pro-UI blue / Desktop layout preview - NOT camera screenshots",24,26);
        String[] files={"live-1-false-false-frame.png","picker-1-1.png","menu-1-3.png","parameter-1.png"};
        String[] labels={"Live view (empty area = camera preview placeholder)","Recipe browser","Settings","Parameter editor"};
        for(int i=0;i<4;i++) {
            int x=24+(i%2)*656,y=68+(i/2)*508;
            g.drawString(labels[i],x,y-10);
            java.awt.image.BufferedImage img=javax.imageio.ImageIO.read(new java.io.File(out+"/"+files[i]));
            g.drawImage(img,x+(640-img.getWidth())/2,y+(480-img.getHeight())/2,null);
        }
        g.dispose();javax.imageio.ImageIO.write(sheet,"png",new java.io.File(out+"/pro-ui-blue-overview.png"));
    }
}
