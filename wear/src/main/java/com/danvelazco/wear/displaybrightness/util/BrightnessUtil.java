package com.danvelazco.wear.displaybrightness.util;

import android.Manifest;
import android.content.Context;
import android.provider.Settings;

/**
 * Utility class for setting brightness level on the watch
 */
public class BrightnessUtil {

    /**
     * Sets the brightness level using the {@link Settings.System}
     *
     * Requires permission {@link Manifest.permission#WRITE_SETTINGS}
     *
     * @param context {@link Context}
     * @param brightnessLevel {@link int} brightness level from 10 to 255
     */
    public static void setSystemBrightnessLevel(Context context, int brightnessLevel) {
        int currentMode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (currentMode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            // Only change system brightness levels if the current mode is not in auto
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessLevel);
        }
    }

}
