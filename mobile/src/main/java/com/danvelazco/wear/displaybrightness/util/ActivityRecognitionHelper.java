package com.danvelazco.wear.displaybrightness.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.danvelazco.wear.displaybrightness.service.ActivityRecognitionIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;

/**
 * Helper class that simply schedules activity detection with the {@link ActivityRecognitionClient} and then
 * disconnects. Call {@link #scheduleActivityUpdates()}
 */
public class ActivityRecognitionHelper implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int DETECTION_INTERVAL_MINUTES = 10;
    public static final int DETECTION_INTERVAL_MILLISECONDS = (DETECTION_INTERVAL_MINUTES * SECONDS_PER_MINUTE)
            * MILLISECONDS_PER_SECOND;
//    public static final int DETECTION_INTERVAL_MILLISECONDS = 10 * MILLISECONDS_PER_SECOND; // Debug

    // Members
    private ActivityRecognitionClient mActivityRecognitionClient;
    private PendingIntent mActivityRecognitionPendingIntent;

    /**
     * Constructor, requires a context that will be used for the {@link PendingIntent} that will be used by the {@link
     * ActivityRecognitionClient}
     *
     * @param context
     *         {@link Context}
     */
    public ActivityRecognitionHelper(Context context) {
        Intent intentService = new Intent(context, ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(context, 0, intentService,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mActivityRecognitionClient = new ActivityRecognitionClient(context, this, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnected(Bundle bundle) {
        if ((mActivityRecognitionClient != null) && mActivityRecognitionClient.isConnected()
                && (mActivityRecognitionPendingIntent != null)) {
            mActivityRecognitionClient.requestActivityUpdates(DETECTION_INTERVAL_MILLISECONDS,
                    mActivityRecognitionPendingIntent);

            // After scheduling the updates, simply disconnect
            mActivityRecognitionClient.disconnect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnected() {
        // No implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // No implementation
    }

    /**
     * Start a connection with the {@link ActivityRecognitionClient}, when it's connected, {@link #onConnected(Bundle)}
     * will be called and then the updates will be scheduled.
     */
    public void scheduleActivityUpdates() {
        if (mActivityRecognitionClient != null) {
            mActivityRecognitionClient.connect();
        }
    }

}
