package com.voxivoid.recipelab;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SonyUiTest {
    @Test void textRolesHaveOneMonotonicHierarchy() {
        assertEquals(10,SonyUi.SECONDARY);assertEquals(12,SonyUi.BODY);assertEquals(14,SonyUi.TITLE);
        assertTrue(SonyUi.SECONDARY<SonyUi.BODY&&SonyUi.BODY<SonyUi.TITLE);
        assertTrue(SonyUi.TITLE<SonyUi.INPUT&&SonyUi.INPUT<SonyUi.NUMBER);
        assertTrue(SonyUi.KEY_LETTER<SonyUi.SECONDARY);
    }
    @Test void commonLayoutUsesAnEvenGrid() {
        for(int value:new int[]{SonyUi.PAGE_PAD,SonyUi.HEADER_HEIGHT,SonyUi.ROW_HEIGHT,SonyUi.INFO_ROW_HEIGHT,
                SonyUi.BADGE_HEIGHT,SonyUi.BADGE_PAD,SonyUi.KEY_GAP,SonyUi.ITEM_GAP,SonyUi.FOOTER_BOTTOM})assertEquals(0,value%2);
        assertTrue(SonyUi.ROW_HEIGHT>SonyUi.BODY*2);
    }
    @Test void photoLegendsNameOnlyTheKeysActuallyHandledInThatMode() {
        assertArrayEquals(new int[]{Keys.I_LEFTRIGHT,Keys.I_ENTER,Keys.I_MENU},Keys.hints(Keys.H_REVIEW,Keys.Caps.UNKNOWN).icons);
        assertArrayEquals(new int[]{Keys.I_MOVE,Keys.I_WHEEL,Keys.I_ENTER,Keys.I_MENU},Keys.hints(Keys.H_REVIEW_ZOOM,Keys.Caps.UNKNOWN).icons);
        assertEquals(Lang.t("action_fit"),Keys.hints(Keys.H_REVIEW_ZOOM,Keys.Caps.UNKNOWN).labels[2]);
    }
    @Test void selectedRowsStayVisibleAcrossWholeLibraries() {
        for (int total : new int[]{1,8,73,1000}) for (int visible : new int[]{1,4,6,8,20}) {
            for (int selected=0; selected<total; selected++) {
                int first = SonyUi.firstRow(total,visible,selected);
                assertTrue(first >= 0 && first <= Math.max(0,total-visible));
                assertTrue(selected >= first && selected < first+visible);
            }
        }
    }
    @Test void rowsNeverExtendIntoReservedFooter() {
        for (float available : new float[]{-20,0,15,30,31,150,213}) {
            int n = SonyUi.visibleRows(available,30);
            assertTrue(n*30 <= Math.max(0,available));
        }
        assertEquals(0,SonyUi.visibleRows(100,0));
    }
    @Test void truncationKeepsSurrogatePairsIntact() {
        String s = "A\uD83D\uDCF7BC";
        assertEquals(1,SonyUi.safeEnd(s,2));
        assertEquals(3,SonyUi.safeEnd(s,3));
        assertEquals(0,SonyUi.safeEnd(s,-1));
        assertEquals(s.length(),SonyUi.safeEnd(s,100));
    }
    @Test void promptHasConfirmAndCancelButNoNonexistentShortcut() {
        Keys.Hints h = Keys.hints(Keys.H_PROMPT,Keys.Caps.UNKNOWN);
        assertArrayEquals(new int[]{Keys.I_ENTER,Keys.I_MENU},h.icons);
        assertEquals(Lang.t("action_cancel"),h.labels[1]);
    }
    @Test void editGuideNamesTheActualLeftRightControl() {
        assertEquals(Keys.I_LEFTRIGHT,Keys.hints(Keys.H_EDIT,Keys.Caps.UNKNOWN).icons[0]);
    }
    @Test void compactPanelRetainsHierarchyWithinThirtyPercentBeforeHintWrapping() {
        for (int[] size:new int[][]{{320,240},{640,480},{800,600},{960,720}}) {
            float height=SonyUi.LIVE_HEIGHT*SonyUi.scale(2,size[0],size[1]);
            assertTrue(height<=size[1]*.30f);
            assertTrue(height>=size[1]*.25f);
            assertTrue(SonyUi.LIVE_QUIET_HEIGHT*SonyUi.scale(2,size[0],size[1])<height);
        }
    }
    @Test void appliedAndPreviewDifferByTextAndBadgeColours() {
        assertEquals(SonyUi.ENABLED,SonyUi.statusFrame(true));
        assertEquals(SonyUi.ON_ACCENT,SonyUi.statusText(true));
        assertEquals(SonyUi.MUTED,SonyUi.statusFrame(false));
        assertNotEquals(SonyUi.statusFrame(true),SonyUi.statusFrame(false));
        assertNotEquals(SonyUi.statusText(true),SonyUi.statusText(false));
    }
    @Test void compactHudNeverHidesWarningsOrParameterEditing() {
        assertTrue(SonyUi.compactPanel(true,0,false,false));
        assertFalse(SonyUi.compactPanel(false,0,false,false));
        assertFalse(SonyUi.compactPanel(true,1,false,false));
        assertFalse(SonyUi.compactPanel(true,0,true,false));
        assertFalse(SonyUi.compactPanel(true,0,false,true));
    }
    @Test void liveGuideDoesNotInventFnWhenItWasNotProbed() {
        Keys.Hints h=Keys.hints(Keys.H_LIVE,Keys.Caps.UNKNOWN);
        assertArrayEquals(new int[]{Keys.I_WHEEL,Keys.I_UPDOWN,Keys.I_ENTER,Keys.I_MENU},h.icons);
        h=Keys.hints(Keys.H_LIVE,new Keys.Caps(true));
        assertEquals(Keys.I_FN,h.icons[3]);
        assertEquals(Lang.t("action_compact_menu"),h.labels[h.labels.length-1]);
    }
    @Test void focusBackgroundAndAppliedStateAreNotTheSameVisualToken() {
        assertNotEquals(SonyUi.ACCENT,SonyUi.SELECTED);
        assertNotEquals(SonyUi.ACCENT,SonyUi.TEXT);
        assertNotEquals(SonyUi.TEXT,SonyUi.ON_ACCENT);
        assertNotEquals(SonyUi.ACCENT,SonyUi.ENABLED);
        assertNotEquals(SonyUi.ATTENTION,SonyUi.ENABLED);
    }
    @Test void statusColoursRemainReadableOnCameraDarkBackground() {
        assertTrue(contrast(SonyUi.ON_ACCENT,SonyUi.ENABLED)>=4.5);
        assertTrue(contrast(SonyUi.MUTED,SonyUi.BACKGROUND)>=4.5);
        assertTrue(contrast(SonyUi.ATTENTION,SonyUi.BACKGROUND)>=4.5);
    }
    private static double contrast(int a,int b) {
        double x=luminance(a),y=luminance(b); return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);
    }
    private static double luminance(int color) {
        double[] c={((color>>16)&255)/255.0,((color>>8)&255)/255.0,(color&255)/255.0};
        for(int i=0;i<3;i++)c[i]=c[i]<=.04045?c[i]/12.92:Math.pow((c[i]+.055)/1.055,2.4);
        return c[0]*.2126+c[1]*.7152+c[2]*.0722;
    }
    @Test void cameraScaleDoesNotDependOnAnUnverifiedFirmwareDpi() {
        assertEquals(2f,SonyUi.scale(1,640,480));
        assertEquals(2f,SonyUi.scale(3,640,480));
        assertEquals(1f,SonyUi.scale(2,320,240));
        assertEquals(2f,SonyUi.scale(2,0,0));
        assertEquals(1f,SonyUi.scale(0,0,0));
    }
}
