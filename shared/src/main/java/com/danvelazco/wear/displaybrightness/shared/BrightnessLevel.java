package com.danvelazco.wear.displaybrightness.shared;

import android.content.ContentResolver;
import android.provider.Settings;

/**
 * Brightness level constant values used for sending between phone app and watch
 */
public class BrightnessLevel {

    /**
     * Constants used for Wear Data API
     */
    public static final String PATH_BRIGHTNESS = "/brightness";
    public static final String FIELD_NAME = "brightness_level";

    /**
     * Values used to determine brightness levels, this way we don't send the actual level we want to set and eventually
     * the watch app can determine the value on its own, in case newer watches have non-standard brightness levels
     */
    public static final int LOWEST = 0;
    public static final int MEDIUM = 1;
    public static final int HIGHEST = 2;

    /**
     * Default brightness levels
     */
    private static final int LOW_LEVEL = 10;
    private static final int MEDIUM_LEVEL = 140;
    private static final int HIGHEST_LEVEL = 255;

    /**
     * Get the brightness level with a known value
     * <p/>
     * Values must be {@link BrightnessLevel#LOWEST}, {@link BrightnessLevel#MEDIUM} or {@link BrightnessLevel#HIGHEST},
     * but if the value is none of that, the {@link BrightnessLevel#MEDIUM} value will be used as default.
     *
     * @param value
     *         {@link int} like {@link #LOWEST}, {@link #MEDIUM} or {@link #HIGHEST}
     * @return {@link int} brightness level to be set using the {@link Settings.System#putInt(ContentResolver, String,
     * int)}
     */
    public static int getBrightnessLevel(int value) {
        // Map the brightness level to each value
        switch (value) {
            case LOWEST:
                return LOW_LEVEL;
            case MEDIUM:
                return MEDIUM_LEVEL;
            case HIGHEST:
                return HIGHEST_LEVEL;
            default:
                // If the value is unknown, simply fallback to medium brightness
                return MEDIUM_LEVEL;
        }
    }

}
