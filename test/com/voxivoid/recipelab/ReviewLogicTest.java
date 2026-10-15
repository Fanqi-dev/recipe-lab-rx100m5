package com.voxivoid.recipelab;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReviewLogicTest {
    @Test void closeKeysNeverTakeAPhoto(){for(int k:new int[]{Keys.K_PLAY,Keys.K_MENU,Keys.K_SK1,Keys.K_S1,517})assertEquals(ReviewLogic.CLOSE,ReviewLogic.command(k,false,false));assertEquals(ReviewLogic.CLOSE,ReviewLogic.command(0,true,true));}
    @Test void deleteResetMovieAndFullShutterDoNothing(){for(int k:new int[]{Keys.K_DELETE,Keys.K_SK2,Keys.K_S2,Keys.K_MOVIE,Keys.K_FN})for(boolean z:new boolean[]{false,true})assertEquals(ReviewLogic.NONE,ReviewLogic.command(k,false,z));}
    @Test void arrowsNavigateBeforeZoomAndPanAfterZoom(){assertEquals(ReviewLogic.PREVIOUS,ReviewLogic.command(Keys.K_LEFT,false,false));assertEquals(ReviewLogic.NEXT,ReviewLogic.command(Keys.K_RIGHT,false,false));assertEquals(ReviewLogic.PAN_LEFT,ReviewLogic.command(Keys.K_LEFT,false,true));assertEquals(ReviewLogic.PAN_DOWN,ReviewLogic.command(Keys.K_DOWN,false,true));}
    @Test void centreEnlargesOrReturnsToFit(){assertEquals(ReviewLogic.ZOOM_IN,ReviewLogic.command(Keys.K_ENTER,false,false));assertEquals(ReviewLogic.FIT,ReviewLogic.command(Keys.K_ENTER,false,true));}
    @Test void wheelNavigatesOrZooms(){assertEquals(ReviewLogic.NEXT,ReviewLogic.command(Keys.K_WHEEL_CW,false,false));assertEquals(ReviewLogic.ZOOM_OUT,ReviewLogic.command(Keys.K_DIAL_CCW,false,true));}
    @Test void zoomLeverIsExplicit(){assertEquals(ReviewLogic.ZOOM_IN,ReviewLogic.command(Keys.K_ZOOM_T,false,false));assertEquals(ReviewLogic.ZOOM_OUT,ReviewLogic.command(Keys.K_ZOOM_W,false,true));}
    @Test void zoomHasFiniteBoundsAndReset(){ReviewLogic m=new ReviewLogic();for(int i=0;i<50;i++)m.changeZoom(1);assertEquals(8,m.zoom());assertFalse(m.changeZoom(1));for(int i=0;i<50;i++)m.changeZoom(-1);assertEquals(1,m.zoom());m.changeZoom(1);m.fit();assertFalse(m.zoomed());}
    @Test void jpegPathsAreAbsoluteCaseInsensitiveAndNotRaw(){assertTrue(ReviewLogic.jpeg("/mnt/sd/DCIM/A.JPEG"));for(String p:new String[]{"A.JPG","/x/A.ARW","/x/A.JPG.tmp","/x/A.MP4","/x/A.JPG\0",null})assertFalse(ReviewLogic.jpeg(p));}
    @Test void staleRequestsCannotPublish(){ReviewLogic.Tickets t=new ReviewLogic.Tickets();int a=t.begin(),b=t.next();assertFalse(t.accepts(a));assertTrue(t.accepts(b));t.close();assertFalse(t.accepts(b));int c=t.begin();assertTrue(t.accepts(c));assertFalse(t.accepts(b));}
    @Test void indexedNavigationSkipsRawAndWraps()throws Exception{String[] p={"/A.JPG","/B.ARW","/C.JPEG"};ReviewLogic.Tickets t=new ReviewLogic.Tickets();int id=t.begin();assertEquals(0,ReviewLogic.find(3,-1,1,i->p[i],t,id));assertEquals(2,ReviewLogic.find(3,0,1,i->p[i],t,id));assertEquals(0,ReviewLogic.find(3,2,1,i->p[i],t,id));assertEquals(2,ReviewLogic.find(3,0,-1,i->p[i],t,id));}
    @Test void emptyOrAllRawHasNoPhoto()throws Exception{ReviewLogic.Tickets t=new ReviewLogic.Tickets();int id=t.begin();assertEquals(-1,ReviewLogic.find(0,-1,1,i->null,t,id));assertEquals(-1,ReviewLogic.find(40,-1,1,i->"/A.ARW",t,id));}
    @Test void cancelledNavigationDoesNotScanRestOfIndex()throws Exception{ReviewLogic.Tickets t=new ReviewLogic.Tickets();int id=t.begin();int[] calls={0};assertEquals(-1,ReviewLogic.find(10000,-1,1,i->{calls[0]++;t.close();return "/A.ARW";},t,id));assertEquals(1,calls[0]);}
}
