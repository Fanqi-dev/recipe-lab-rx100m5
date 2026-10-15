package com.voxivoid.recipelab;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SonyAppSessionTest {
    @Test void playbackStaysInAppAndOnlyUsbPullsBack() {
        assertArrayEquals(new String[]{"KEY_USB_CONNECT"},SonyAppSession.pullingBackKeys());
    }
    @Test void previousResumeEventsAreRetainedWithoutInventedEvents() {
        List<String> keys=Arrays.asList(SonyAppSession.resumeKeys());
        assertTrue(keys.contains("KEY_PLAY_APO")); assertTrue(keys.contains("KEY_PLAY_PON"));
        assertTrue(keys.contains("KEY_RELEASE_APO")); assertEquals(9,new HashSet<String>(keys).size());
    }
    @Test void callersCannotMutateSessionArrays() {
        SonyAppSession.pullingBackKeys()[0]="changed"; SonyAppSession.resumeKeys()[0]="changed";
        assertEquals("KEY_USB_CONNECT",SonyAppSession.pullingBackKeys()[0]);
        assertEquals("KEY_POWER_SLIDE_PON",SonyAppSession.resumeKeys()[0]);
    }
}
