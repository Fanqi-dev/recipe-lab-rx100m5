package com.voxivoid.recipelab;
import android.content.Context;
import android.graphics.*;
import java.util.*;
/** Desktop-only geometry preview of production Canvas views, NOT an Android/camera runtime. */
public class UiPreview {
    static String out;
    static int checks;
    static int alignmentChecks;
    static int roleChecks;
    public static void main(String[] args) throws Exception {
        out=args[0]; new java.io.File(out).mkdirs();
        for(int lang=0;lang<3;lang++) {
            Lang.use(lang);
            Typeface face=lang==0?Typeface.DEFAULT:Typeface.from("assets/fonts/RecipeLabCJK"+(lang==1?"sc":"tc")+"-Regular.ttf");
            alignment(face);
            for(int selected=0;selected<DevTools.APP_ROWS;selected++) {
                MenuView v=new MenuView(new Context(),null);v.setTypeface(face);
                String[] labels=new String[DevTools.APP_ROWS],details=labels.clone(),values=labels.clone();
                for(int i=0;i<labels.length;i++){labels[i]=DevTools.appLabel(i);details[i]=DevTools.appDetail(i);values[i]=DevTools.appValue(i,0,lang);}
                int mode=values[selected]==null?Keys.H_MENU_TOP:Keys.H_MENU_TOP_VALUE;
                v.set(Lang.t("menu_title"),labels,details,values,null,DevTools.icons(0),selected,Keys.hints(mode,Keys.Caps.UNKNOWN));
                check(v,"menu-"+lang+"-"+selected);
            }
            Library lib=new Library();
            for(int i=0;i<Recipes.ALL.length;i++){
                int[] values=new int[Params.N];values[Params.R_QUAL]=Params.Q_FINE;values[Params.R_WBMODE]=Params.WB_AUTO;Params.stage(lib.get(i),values);
                int[] before=values.clone();String info=LiveRecipeView.summary(values);
                if(!Arrays.equals(before,values))throw new AssertionError("Display summary mutated staged parameters");
                Paint small=new Paint();small.setTypeface(face);small.setTextSize(2*SonyUi.SECONDARY);
                if(small.measureText(info)>592)throw new AssertionError("Summary would hide WB/EV: "+info);
                int look=values[Params.R_PE]==0?Params.R_STYLE:Params.R_PE;
                if(!info.startsWith(Params.fmt(look,values[look],values)))throw new AssertionError("Wrong style/effect summary");
            }
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
                LiveRecipeView live=new LiveRecipeView(new Context(),null);live.setTypeface(face);
                int[] model=new int[Params.ROW_NAME.length]; model[Params.R_WBMODE]=Params.WB_AUTO;
                Params.stage(recipe,model);
                String wb=model[Params.R_WBMODE]==Params.WB_KELVIN?Params.fmt(Params.R_KELVIN,model[Params.R_KELVIN],model):Params.fmt(Params.R_WBMODE,model[Params.R_WBMODE],model);
                String summary=Params.fmt(Params.R_STYLE,model[Params.R_STYLE],model)+"   WB "+wb+"   EV "+Params.fmt(Params.R_EV,model[Params.R_EV],model);
                live.set(Recipes.displayName(recipe),Recipes.groupLabel(recipe.group),lib.position(index),summary,applied,false,new Keys.Caps(true),!quiet);
                live.measure(640,480);
                if(live.getHeight()<(quiet?108:144)||live.getHeight()>188)throw new AssertionError("Unexpected compact panel height: "+live.getHeight());
                if(lang!=0&&!quiet&&live.getHeight()!=144)throw new AssertionError("Chinese panel should occupy 144px: "+live.getHeight());
                String name="live-"+lang+"-"+applied+"-"+quiet;
                check(live,name);
                // Neutral area represents the missing hardware SurfaceView; it is NOT a camera photo.
                java.awt.image.BufferedImage frame=new java.awt.image.BufferedImage(640,480,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g=frame.createGraphics();g.setColor(new java.awt.Color(32,32,32));g.fillRect(0,0,640,480);
                g.drawImage(javax.imageio.ImageIO.read(new java.io.File(out+"/"+name+".png")),0,480-live.getHeight(),null);g.dispose();
                javax.imageio.ImageIO.write(frame,"png",new java.io.File(out+"/"+name+"-frame.png"));
            }
            int caseId=0;
            for(String text:new String[]{"Classic Chrome", "A very long custom recipe name that must not cover the state badge", "自定义配方名称测试与已启用状态不能重叠"})
                for(boolean applied:new boolean[]{false,true}) for(boolean edited:new boolean[]{false,true}) {
                    LiveRecipeView live=new LiveRecipeView(new Context(),null);live.setTypeface(face);
                    live.set(text,Lang.t("menu_browse"),"77 / 77","WB 5500K   EV +0.3",applied,edited,new Keys.Caps(true),true);live.measure(640,480);
                    check(live,"live-long-"+lang+"-"+(caseId++));
                }
            ParameterCardView card=new ParameterCardView(new Context(),null);card.setTypeface(face);
            int reviewCase=0;
            for(String text:new String[]{"DSC00001.JPG","A very long filename for the image that must never cover zoom.jpg",Lang.t("review_loading"),Lang.t("review_empty"),Lang.t("review_unavailable"),Lang.t("review_returning")})
                for(float zoom:new float[]{1,2,8}){
                    PhotoReviewChrome review=new PhotoReviewChrome(new Context(),null);review.setTypeface(face);review.set(text,zoom);review.measure(640,480);check(review,"review-"+lang+"-"+(reviewCase++));
                }
            Paint reviewText=new Paint();reviewText.setTypeface(face);reviewText.setTextSize(20);
            for(String key:new String[]{"review_keys","review_zoom_keys"})if(reviewText.measureText(Lang.t(key))>600)throw new AssertionError("Essential review controls would be truncated: "+key+" language "+lang);
            card.set(Params.rowName(Params.R_KELVIN),"5500K");card.measure(640,480);check(card,"parameter-"+lang);
            PromptView p=new PromptView(new Context(),null);p.setTypeface(face);
            p.set(DevTools.resetTitle(),DevTools.resetBody(),DevTools.resetOptions(),DevTools.RESET_DEFAULT,null);
            p.measure(640,480);check(p,"reset-"+lang);
            String longName="RX100M5 custom recipe with a long but valid name";
            String[][] questions={{CustomRecipes.discardTitle(longName),CustomRecipes.discardBody()},
                    {CustomRecipes.deleteTitle(longName),CustomRecipes.deleteBody("LONG-NAME.RECIPE")},
                    {longName,CustomRecipes.optionsBody("LONG-NAME.RECIPE")}};
            String[][] answers={CustomRecipes.discardOptions(),CustomRecipes.deleteOptions(),CustomRecipes.options()};
            for(int q=0;q<questions.length;q++)for(int selected=0;selected<answers[q].length;selected++){
                p.set(questions[q][0],questions[q][1],answers[q],selected,null);p.measure(640,480);
                Canvas qc=draw(p);
                for(String answer:answers[q])if(text(qc,answer).size!=2*SonyUi.BODY)throw new AssertionError("A confirmation option was shrunk or truncated");
                check(p,"question-"+lang+"-"+q+"-"+selected);
            }
            KeyboardView k=new KeyboardView(new Context(),null);k.setTypeface(face);
            k.set(Lang.t("menu_new"),NameEntry.blank("New recipe"),Keys.hints(Keys.H_NAME,Keys.Caps.UNKNOWN));check(k,"keyboard-"+lang);
        }
        Paint paint=new Paint();paint.setTextSize(20);
        for(String s:new String[]{"short","A very long custom recipe filename that needs trimming", "照片配方菜单"})
            for(float width:new float[]{0,5,20,40,100,200})
                if(paint.measureText(CanvasText.fit(paint,s,width))>width+.1)throw new AssertionError("Text overflow");
        System.out.println(checks+" source-driven desktop layout previews passed bounds checks. Not camera screenshots.");
        System.out.println(alignmentChecks+" font-metric alignment assertions passed across three languages.");
        System.out.println(roleChecks+" text runs use shared type roles without synthetic bold.");
        overview();
    }
    static Canvas draw(android.view.View v){Canvas c=new Canvas(v.getWidth(),v.getHeight());v.draw(c);return c;}
    static Canvas.TextRun text(Canvas c,String value){for(Canvas.TextRun r:c.texts)if(r.text.equals(value))return r;throw new AssertionError("Missing label: "+value);}
    static void equal(float a,float b){alignmentChecks++;if(Math.abs(a-b)>.01f)throw new AssertionError("Alignment mismatch: "+a+" != "+b);}
    static void alignment(Typeface face){
        LiveRecipeView live=new LiveRecipeView(new Context(),null);live.setTypeface(face);live.measure(640,480);
        Paint state=new Paint();state.setTypeface(face);state.setTextSize(20);
        float badgeWidth=CanvasText.statusWidth(state,2),badgeCenter=640-24-badgeWidth/2;
        for(boolean applied:new boolean[]{false,true}){
            live.set("Recipe","Group","9 / 77","WB 5500K   EV 0",applied,false,new Keys.Caps(true),true);live.measure(640,480);Canvas c=draw(live);
            Canvas.TextRun name=text(c,"Recipe"),status=text(c,Lang.t(applied?"state_active":"state_preview"));
            equal(name.x,24);equal(name.centerY,54);equal(status.centerY,name.centerY);
            equal(status.x+status.width/2,badgeCenter);
            equal(text(c,"Group").x,name.x);equal(text(c,"9 / 77").x+text(c,"9 / 77").width,616);
            equal(text(c,"Group").centerY,text(c,"9 / 77").centerY);
            equal(text(c,"WB 5500K").x,name.x);equal(text(c,"WB 5500K").centerY,text(c,"EV 0").centerY);
            text(c,Lang.t("action_pick"));text(c,Lang.t("action_compact_menu"));
            if(name.size<=text(c,"Group").size||name.size<=status.size)throw new AssertionError("Name has no type hierarchy");
            if(text(c,"Group").bottom>name.top+.1f||name.bottom>text(c,"WB 5500K").top+.1f)
                throw new AssertionError("Home information rows overlap");
            if(text(c,"WB 5500K").bottom>text(c,Lang.t("action_pick")).top+.1f)
                throw new AssertionError("Guide overlaps summary");
        }
        MenuView menu=new MenuView(new Context(),null);menu.setTypeface(face);
        menu.set("Header",new String[]{"Row","Other row"},new String[]{"",""},new String[]{"Value","X"},null,null,0,Keys.hints(Keys.H_MENU_TOP,Keys.Caps.UNKNOWN));
        Canvas m=draw(menu);Canvas.TextRun header=text(m,"Header");equal(header.x,24);equal(header.centerY,28);
        equal(text(m,"Row").centerY,text(m,"Value").centerY);
        equal(text(m,"Other row").centerY-text(m,"Row").centerY,2*SonyUi.ROW_HEIGHT);
        equal(text(m,"Row").size,2*SonyUi.BODY);equal(header.size,2*SonyUi.TITLE);
        equal(text(m,"Value").x+text(m,"Value").width/2,text(m,"X").x+text(m,"X").width/2);
        KeyboardView keyboard=new KeyboardView(new Context(),null);keyboard.setTypeface(face);
        keyboard.set("Header",NameEntry.blank("Recipe"),Keys.hints(Keys.H_NAME,Keys.Caps.UNKNOWN));
        Canvas.TextRun k=text(draw(keyboard),"Header");equal(k.x,header.x);equal(k.y,header.y);
        Library lib=new Library();int index=Favourites.recipeAt(1,0,Collections.<Integer>emptyList(),lib);
        PickerView picker=new PickerView(new Context(),null);picker.setTypeface(face);
        for(boolean applied:new boolean[]{false,true}){
            picker.set(index,1,1,Collections.<Integer>emptyList(),lib,true,applied);Canvas c=draw(picker);
            Canvas.TextRun title=text(c,Lang.t("menu_browse")),status=text(c,Lang.t(applied?"state_active":"state_preview"));
            equal(title.x,header.x);equal(title.y,header.y);equal(status.x+status.width/2,badgeCenter);
        }
        ParameterCardView parameter=new ParameterCardView(new Context(),null);parameter.setTypeface(face);parameter.set("Parameter","5500K");parameter.measure(640,480);
        Canvas p=draw(parameter);equal(text(p,"Parameter").centerY,text(p,Lang.t("action_edit")).centerY);
        equal(text(p,"5500K").centerY,116);
        PhotoReviewChrome review=new PhotoReviewChrome(new Context(),null);review.setTypeface(face);review.set("DSC00001.JPG",2);review.measure(640,480);
        Canvas r=draw(review);equal(text(r,"DSC00001.JPG").x,header.x);equal(text(r,"DSC00001.JPG").centerY,text(r,"2x").centerY);
        equal(text(r,Lang.t("action_pan")).size,2*SonyUi.SECONDARY);
        equal(text(r,Lang.t("action_back")).centerY,text(r,Lang.t("action_pan")).centerY);
        Legend legend=new Legend(2);legend.setTypeface(face);
        Canvas guide=new Canvas(640,100);legend.drawWrapped(guide,24,0,592,Keys.hints(Keys.H_LIVE,new Keys.Caps(true)));
        for(String key:new String[]{"Fn","MENU"}){
            Canvas.TextRun run=text(guide,key);Canvas.RectRun frame=null;
            for(Canvas.RectRun rect:guide.rects)if(rect.round&&run.x>=rect.left&&run.x+run.width<=rect.right&&Math.abs(run.centerY-(rect.top+rect.bottom)/2)<.01f){frame=rect;break;}
            if(frame==null)throw new AssertionError("Key lettering escaped its keycap: "+key);
            equal(frame.bottom-frame.top,2*SonyUi.KEY_SIZE);equal(run.size,2*SonyUi.KEY_LETTER);
            Canvas.TextRun label=text(guide,Lang.t(key.equals("Fn")?"action_browse":"action_compact_menu"));
            equal(label.centerY,run.centerY);equal(label.x-frame.right,2*SonyUi.KEY_GAP);equal(label.size,2*SonyUi.SECONDARY);
        }
    }
    static void check(android.view.View v,String name)throws Exception {
        Canvas c=new Canvas(v.getWidth(),v.getHeight());v.draw(c);
        if(!c.overflow.isEmpty())throw new AssertionError(name+" text outside viewport: "+c.overflow);
        if(v.getHeight()>480)throw new AssertionError(name+" too tall: "+v.getHeight());
        for(Canvas.TextRun run:c.texts){
            boolean known=false;for(int role:new int[]{SonyUi.TITLE,SonyUi.BODY,SonyUi.SECONDARY,SonyUi.INPUT,SonyUi.NUMBER,SonyUi.KEY_LETTER})if(Math.abs(run.size-role*2)<.01f)known=true;
            if(!known||run.bold)throw new AssertionError(name+" has an ad-hoc type style: "+run.text+" size="+run.size+" bold="+run.bold);
            if(v instanceof PromptView&&!run.text.isEmpty()&&",.;:!?，。；：！？、）】》」』〉〕’”".indexOf(run.text.charAt(0))>=0)
                throw new AssertionError(name+" strands punctuation at line start: "+run.text);
            roleChecks++;
        }
        c.save(out+"/"+name+".png");checks++;
    }
    static void overview()throws Exception {
        java.awt.image.BufferedImage sheet=new java.awt.image.BufferedImage(1328,1592,java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g=sheet.createGraphics();
        g.setColor(new java.awt.Color(20,20,20));g.fillRect(0,0,1328,1592);
        g.setFont(new java.awt.Font("SansSerif",0,18));g.setColor(java.awt.Color.LIGHT_GRAY);
        g.drawString("Recipe Lab / Pro-UI blue / Desktop layout preview - NOT camera screenshots",24,26);
        String[] files={"live-1-false-false-frame.png","picker-1-1.png","menu-1-3.png","parameter-1.png","keyboard-1.png","reset-1.png"};
        String[] labels={"Live view (empty area = camera preview placeholder)","Recipe browser","Settings","Parameter editor","Name entry","Confirmation (Cancel selected)"};
        for(int i=0;i<files.length;i++) {
            int x=24+(i%2)*656,y=68+(i/2)*508;
            g.drawString(labels[i],x,y-10);
            java.awt.image.BufferedImage img=javax.imageio.ImageIO.read(new java.io.File(out+"/"+files[i]));
            g.drawImage(img,x+(640-img.getWidth())/2,y+(480-img.getHeight())/2,null);
        }
        g.dispose();javax.imageio.ImageIO.write(sheet,"png",new java.io.File(out+"/pro-ui-blue-overview.png"));
    }
}
