package com.danvelazco.wear.displaybrightness.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.danvelazco.wear.displaybrightness.service.ActivityRecognitionIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

/**
 * Helper class that simply schedules activity detection with the {@link ActivityRecognition#API} and then disconnects.
 * Call {@link #scheduleActivityUpdates()}
 */
public class ActivityRecognitionHelper implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int DETECTION_INTERVAL_MINUTES = 10;
    public static final int DETECTION_INTERVAL_MILLISECONDS = (DETECTION_INTERVAL_MINUTES * SECONDS_PER_MINUTE)
            * MILLISECONDS_PER_SECOND;
//    public static final int DETECTION_INTERVAL_MILLISECONDS = 10 * MILLISECONDS_PER_SECOND; // Debug

    // Members
    private GoogleApiClient mGoogleApiClient = null;
    private PendingIntent mActivityRecognitionPendingIntent;

    /**
     * Constructor, requires a context that will be used for the {@link PendingIntent} used by the {@link
     * ActivityRecognition#API}
     *
     * @param context
     *         {@link Context}
     */
    public ActivityRecognitionHelper(Context context) {
        Intent intentService = new Intent(context, ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(context, 0, intentService,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnected(Bundle bundle) {
        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()
                && (mActivityRecognitionPendingIntent != null)) {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient,
                    DETECTION_INTERVAL_MILLISECONDS, mActivityRecognitionPendingIntent);

            // After scheduling the updates, simply disconnect
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionSuspended(int i) {
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
     * Start a connection with the {@link ActivityRecognition#API}, when it's connected, {@link #onConnected(Bundle)}
     * will be called and then the updates will be scheduled.
     */
    public void scheduleActivityUpdates() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

}
