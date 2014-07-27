package com.danvelazco.wear.displaybrightness.service;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import com.danvelazco.wear.displaybrightness.shared.BrightnessLevel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Intent service used by the {@link ActivityRecognitionClient} to receive the current {@link DetectedActivity} the user
 * is doing at the moment.
 * <p/>
 * When this {@link IntentService} starts, we fetch the {@link DetectedActivity} via {@link #onHandleIntent(Intent)},
 * determine the user's last known location using the {@link LocationClient#getLastLocation()}, calculate the user's
 * sunrise/sunset times, and determine the proper {@link BrightnessLevel} and send it to the wearable using the Data
 * API.
 */
public class ActivityRecognitionIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GooglePlayServicesClient.ConnectionCallbacks {

    // Constants
    private static final String LOG_TAG = "ActivityRecognitionIntentService";

    // Members
    private GoogleApiClient mGoogleApiClient;
    private LocationClient mLocationClient;

    // Pending data
    private DetectedActivity mDetectedActivity = null;
    private Location mCurrentLocation = null;
    private int mPendingLevelToSend = -1;

    /**
     * Constructor
     */
    public ActivityRecognitionIntentService() {
        super(LOG_TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("ActivityRecognitionIntentService", "onCreate()");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (mLocationClient != null && mLocationClient.isConnected() && (mPendingLevelToSend == -1)) {
            if (mLocationClient.getLastLocation() != null) {
                mCurrentLocation = mLocationClient.getLastLocation();

                // Synchronize the activity detection and the user's location before sending data to wearable
                if (mDetectedActivity != null) {
                    determineBrightnessLevelBasedOnData();
                } else {
                    Log.d(LOG_TAG, "Location data is available but we haven't detected activity yet, waiting...");
                }
            }
        }

        // If the client is connected and we have pending data to be sent, simply send it
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && (mPendingLevelToSend != -1)) {
            sendBrightnessLevelToWatch(mPendingLevelToSend);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnected() {
        // Not implemented
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionSuspended(int i) {
        // Not implemented
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Not implemented
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("ActivityRecognitionIntentService", "onHandleIntent()");
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        if (result != null) {
            DetectedActivity detectedActivity = result.getMostProbableActivity();
            if (detectedActivity != null) {
                Log.d(LOG_TAG, "Detected activity: " + detectedActivity.toString());
                mDetectedActivity = detectedActivity;

                // Synchronize the activity detection and the user's location before sending data to wearable
                if (mCurrentLocation != null) {
                    determineBrightnessLevelBasedOnData();
                } else {
                    Log.d(LOG_TAG, "Detected activity but We don't have location data yet, waiting...");
                }
            }
        }
    }

    /**
     * Disconnect all connected clients
     */
    private void disconnectAll() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
        if (mLocationClient != null && mLocationClient.isConnected()) {
            mLocationClient.disconnect();
            mLocationClient = null;
        }
    }

    /**
     * Determine the brightness level that will be set on the watch based on the received data (Detected activity and
     * last known location.
     */
    private void determineBrightnessLevelBasedOnData() {
        Log.d(LOG_TAG, "determineBrightnessLevelBasedOnData()");

        if (!isDaytime()) {
            Log.d(LOG_TAG, "It's night time");

            // It's nighttime, nothing to see here, set it to lowest and forget it!
            setBrightnessLevel(BrightnessLevel.LOWEST);
            return;
        } else {
            Log.d(LOG_TAG, "It's day time");
        }

        int brightnessLevel;
        switch (mDetectedActivity.getType()) {
            case DetectedActivity.STILL:
            case DetectedActivity.TILTING:
            case DetectedActivity.UNKNOWN:
                Log.d(LOG_TAG, "Mild activity, we may be indoors, setting to medium");
                brightnessLevel = BrightnessLevel.MEDIUM;
                break;
            case DetectedActivity.IN_VEHICLE:
            case DetectedActivity.ON_BICYCLE:
            case DetectedActivity.ON_FOOT:
            case DetectedActivity.WALKING:
            case DetectedActivity.RUNNING:
                Log.d(LOG_TAG, "Outdoors activity, setting to high");
                brightnessLevel = BrightnessLevel.HIGHEST;
                break;
            default:
                Log.d(LOG_TAG, "I have no idea what I'm doing");
                brightnessLevel = BrightnessLevel.MEDIUM;
                break;
        }

        setBrightnessLevel(brightnessLevel);
    }

    /**
     * Whether ir currently day or night time. Uses the {@link SunriseSunsetCalculator} to find out the sunrise and
     * sunset times of the place where the user currently is at.
     *
     * @return {@link boolean} true if it's currently day time, false if night time
     */
    private boolean isDaytime() {
        Log.d(LOG_TAG, "isDaytime()");

        if (mCurrentLocation != null) {
            com.luckycatlabs.sunrisesunset.dto.Location ssLocation = new com.luckycatlabs.sunrisesunset.dto.Location(mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude());
            SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(ssLocation, TimeZone.getDefault());

            Calendar calendarNow = Calendar.getInstance();
            Calendar calendarSunrise = calculator.getOfficialSunriseCalendarForDate(Calendar.getInstance());
            Calendar calendarSunset = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());

            // It's day time if the current time is after sunrise and before sunset
            return calendarNow.after(calendarSunrise) && calendarNow.before(calendarSunset);
        }

        // If we couldn't calculate the sunrise/sunset, default to day time
        return true;
    }

    /**
     * Possibly send the brightness level to the watch, unless we are still not connected to the {@link
     * GoogleApiClient}, if so, simply keep a reference of the brightness level we want to set to be send later when the
     * client connects.
     *
     * @param brightnessLevel
     *         {@link BrightnessLevel} to be sent to the watch
     */
    private void setBrightnessLevel(int brightnessLevel) {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            sendBrightnessLevelToWatch(brightnessLevel);
        } else {
            mPendingLevelToSend = brightnessLevel;
        }
    }

    /**
     * Use the {@link GoogleApiClient} to send a data item to the wearable using the Wear Data API
     *
     * @param level
     *         {@link BrightnessLevel} to be sent to the watch
     */
    private void sendBrightnessLevelToWatch(int level) {
        Log.d(LOG_TAG, "sendBrightnessLevelToWatch(level=" + level + ")");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            PutDataMapRequest dataMap = PutDataMapRequest.create(BrightnessLevel.PATH_BRIGHTNESS);
            dataMap.getDataMap().putInt(BrightnessLevel.FIELD_NAME, level);
            PutDataRequest request = dataMap.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
            Log.d(LOG_TAG, "Data sent to watch");
        }
        disconnectAll();
    }

}
