package com.voxivoid.recipelab;

/** Interoperability constants observed in the supplied Sony Time-lapse 3.40 APK, not Sony implementation code. */
final class SonyAppSession {
    static final String ACTION = "com.android.server.DAConnectionManagerService.AppInfoReceive";
    // PLAY stays in the Activity's native review state; only USB pulls back to the system.
    static String[] pullingBackKeys() { return new String[] { "KEY_USB_CONNECT" }; }
    static String[] resumeKeys() { return new String[] {
        "KEY_POWER_SLIDE_PON", "KEY_RELEASE_APO", "KEY_PLAY_APO", "KEY_MEDIA_INOUT_APO",
        "KEY_LENS_APO", "KEY_ACCESSORY_APO", "KEY_DEDICATED_APO", "KEY_POWER_APO", "KEY_PLAY_PON"
    }; }
}
