package com.danvelazco.wear.displaybrightness.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.danvelazco.wear.displaybrightness.BrightnessLevelsPreferenceActivity;
import com.danvelazco.wear.displaybrightness.service.ScreenMonitorService;
import com.danvelazco.wear.displaybrightness.util.ActivityRecognitionHelper;

/**
 * Broadcast receiver that runs on each boot used to schedule activity recognition updates
 */
public class BootReceiver extends BroadcastReceiver {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(intent.getAction())) {
            ActivityRecognitionHelper activityRecognitionHelper = new ActivityRecognitionHelper(context);
            activityRecognitionHelper.scheduleActivityUpdates();

            SharedPreferences sharedPreferences = context.getSharedPreferences(BrightnessLevelsPreferenceActivity.KEY_PREF_FILENAME, Context.MODE_MULTI_PROCESS);
            if (sharedPreferences.getBoolean(BrightnessLevelsPreferenceActivity.KEY_LINK_BRIGHTNESS, false)){
                Intent screenMonitorService = new Intent(context, ScreenMonitorService.class);
                context.startService(screenMonitorService);
            }
        }
    }
}
