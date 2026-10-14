package com.voxivoid.recipelab;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SonyUiTest {
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
    @Test void photoReviewOnlyNamesImplementedNavigationAndReturn() {
        Keys.Hints h=Keys.hints(Keys.H_REVIEW,Keys.Caps.UNKNOWN);
        assertArrayEquals(new int[]{Keys.I_LEFTRIGHT,Keys.I_MENU},h.icons);
        for (int icon:h.icons) assertNotEquals(Keys.I_TRASH,icon);
        assertEquals(Lang.t("action_back"),h.labels[1]);
    }
    @Test void editGuideNamesTheActualLeftRightControl() {
        assertEquals(Keys.I_LEFTRIGHT,Keys.hints(Keys.H_EDIT,Keys.Caps.UNKNOWN).icons[0]);
    }
    @Test void reviewHalfPressReturnsWithoutTreatingFullPressOrDeleteAsACommand() {
        for (int scan:new int[]{Keys.K_S1,Keys.K_PLAY,Keys.K_MENU,Keys.K_SK1}) assertTrue(Keys.reviewCloses(scan,false));
        for (int scan:new int[]{Keys.K_S2,Keys.K_DELETE,Keys.K_ENTER,Keys.K_LEFT,Keys.K_RIGHT}) assertFalse(Keys.reviewCloses(scan,false));
        assertTrue(Keys.reviewCloses(0,true));
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
    }
    @Test void cameraScaleDoesNotDependOnAnUnverifiedFirmwareDpi() {
        assertEquals(2f,SonyUi.scale(1,640,480));
        assertEquals(2f,SonyUi.scale(3,640,480));
        assertEquals(1f,SonyUi.scale(2,320,240));
        assertEquals(2f,SonyUi.scale(2,0,0));
        assertEquals(1f,SonyUi.scale(0,0,0));
    }
}
