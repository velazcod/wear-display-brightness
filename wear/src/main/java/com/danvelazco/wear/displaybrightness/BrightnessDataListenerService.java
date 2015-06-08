package com.danvelazco.wear.displaybrightness;

import android.net.Uri;
import android.util.Log;
import com.danvelazco.wear.displaybrightness.shared.BrightnessLevel;
import com.danvelazco.wear.displaybrightness.util.BrightnessUtil;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

/**
 * Service used to listen for data changes on the Wear Data API layer and change the display brightness based on the
 * {@link BrightnessLevel} the phone sends.
 */
public class BrightnessDataListenerService extends WearableListenerService {

    // Constants
    private static final String LOG_TAG = "BrightnessDataListener";

    /**
     * Listen for data changes on the {@link BrightnessLevel#PATH_BRIGHTNESS}
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();
            if (BrightnessLevel.PATH_BRIGHTNESS.equals(uri.getPath())) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    Log.d(LOG_TAG, BrightnessLevel.PATH_BRIGHTNESS + " data TYPE_CHANGED");
                    DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    int brightnessLevel = dataMap.getInt(BrightnessLevel.FIELD_NAME, BrightnessLevel.MEDIUM);
                    setBrightness(brightnessLevel);
                }
            }
        }
    }

    /**
     * Set the brightness based on the value set.
     *
     * @param value
     *         {@link int} values must be: {@link BrightnessLevel#LOWEST}, {@link BrightnessLevel#MEDIUM} or {@link
     *         BrightnessLevel#HIGHEST}
     */
    private void setBrightness(int value) {
        int brightnessLevel = BrightnessLevel.getBrightnessLevel(value);
        Log.d(LOG_TAG, "\tBrightness value: " + value + " (level=" + brightnessLevel + ")");
        BrightnessUtil.setSystemBrightnessLevel(this, brightnessLevel);
    }

}
