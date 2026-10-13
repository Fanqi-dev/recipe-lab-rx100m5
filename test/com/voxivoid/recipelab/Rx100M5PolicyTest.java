package com.voxivoid.recipelab;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.voxivoid.recipelab.Params.*;
import java.util.Map;
class Rx100M5PolicyTest {
    int[] rows() { int[] r=new int[N]; Params.stage(Recipes.ALL[0],r); r[R_QUAL]=Q_FINE; return r; }
    @Test void previewNeverChangesFormatOrJpegCompression() {
        int[] r=rows();
        for(int q=0;q<4;q++) { r[R_QUAL]=q; Map<String,String> p=Rx100M5Policy.preview(r,"daylight"); assertFalse(p.containsKey("storage-fmt"));assertFalse(p.containsKey("jpeg-quality")); }
    }
    @Test void presetWhiteBalanceRestoresCorrectlyAfterAutoPreview() {
        int[] r=rows(); r[R_WBMODE]=2;
        assertEquals("daylight",Rx100M5Policy.preview(r,"daylight").get("whitebalance"));
    }
    @Test void selectedAutoIsNotOverriddenByOriginalPreset() {
        assertEquals("auto",Rx100M5Policy.preview(rows(),"daylight").get("whitebalance"));
    }
    @Test void unavailablePresetNameBlocksPreviewInsteadOfGuessing() {
        int[] r=rows(); r[R_WBMODE]=2;
        assertThrows(IllegalStateException.class,()->Rx100M5Policy.preview(r,null));
    }
    @Test void rawBlocksPictureEffectsButNotCreativeStyles() {
        int[] r=rows();r[R_QUAL]=Q_RAW;assertTrue(Rx100M5Policy.effectAllowed(r));
        r[R_PE]=7;assertFalse(Rx100M5Policy.effectAllowed(r));r[R_QUAL]=Q_RAWJPG;assertFalse(Rx100M5Policy.effectAllowed(r));r[R_QUAL]=Q_FINE;assertTrue(Rx100M5Policy.effectAllowed(r));
    }
    @Test void confirmationDefaultsToCancel() { assertEquals(1,Rx100M5Policy.CONFIRM_DEFAULT); }
}
