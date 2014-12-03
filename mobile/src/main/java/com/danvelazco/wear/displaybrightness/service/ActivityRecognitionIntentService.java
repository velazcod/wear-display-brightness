package com.danvelazco.wear.displaybrightness.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import com.danvelazco.wear.displaybrightness.BrightnessLevelsPreferenceActivity;
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
    private SharedPreferences mSharedPreferencesBrightnessLevels;
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

        mSharedPreferencesBrightnessLevels = getSharedPreferences(BrightnessLevelsPreferenceActivity.KEY_PREF_FILENAME,
                MODE_MULTI_PROCESS);

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
            Log.d(LOG_TAG, "It's night time, set to lowest level");

            // It's nighttime, nothing to see here, set it to lowest and forget it!
            setBrightnessLevel(BrightnessLevel.LOWEST);
            return;
        } else {
            Log.d(LOG_TAG, "It's day time, get current activity");
        }

        String brightnessLevelPrefKey;
        String defaultValue;
        switch (mDetectedActivity.getType()) {
            case DetectedActivity.IN_VEHICLE:
                Log.d(LOG_TAG, "Vehicle");
                brightnessLevelPrefKey = BrightnessLevelsPreferenceActivity.KEY_LEVEL_DRIVING;
                defaultValue = Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_DRIVING);
                break;
            case DetectedActivity.ON_BICYCLE:
                Log.d(LOG_TAG, "On bicycle");
                brightnessLevelPrefKey = BrightnessLevelsPreferenceActivity.KEY_LEVEL_ON_BICYCLE;
                defaultValue = Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_BICYCLE);
                break;
            case DetectedActivity.WALKING:
                Log.d(LOG_TAG, "Walking");
                brightnessLevelPrefKey = BrightnessLevelsPreferenceActivity.KEY_LEVEL_WALKING;
                defaultValue = Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_WALKING);
                break;
            case DetectedActivity.RUNNING:
                Log.d(LOG_TAG, "Running");
                brightnessLevelPrefKey = BrightnessLevelsPreferenceActivity.KEY_LEVEL_RUNNING;
                defaultValue = Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_RUNNING);
                break;
            case DetectedActivity.STILL:
                Log.d(LOG_TAG, "Still");
                brightnessLevelPrefKey = BrightnessLevelsPreferenceActivity.KEY_LEVEL_STILL;
                defaultValue = Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_STILL);
                break;
            case DetectedActivity.ON_FOOT:
                Log.d(LOG_TAG, "On foot");
                brightnessLevelPrefKey = BrightnessLevelsPreferenceActivity.KEY_LEVEL_ON_FOOT;
                defaultValue = Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_ON_FOOT);
                break;
            case DetectedActivity.TILTING:
                Log.d(LOG_TAG, "Tilting");
                // Nope... Ignored tilting!
                return;
            case DetectedActivity.UNKNOWN:
                Log.d(LOG_TAG, "Unknown");
                brightnessLevelPrefKey = BrightnessLevelsPreferenceActivity.KEY_LEVEL_UNKNOWN;
                defaultValue = Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_UNKNOWN);
                break;
            default:
                Log.d(LOG_TAG, "I have no idea what I'm doing");
                brightnessLevelPrefKey = BrightnessLevelsPreferenceActivity.KEY_LEVEL_UNKNOWN;
                defaultValue = Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_UNKNOWN);
                break;
        }

        String brightnessLevelValueString = mSharedPreferencesBrightnessLevels. getString(brightnessLevelPrefKey,
                defaultValue);
        setBrightnessLevel(Integer.parseInt(brightnessLevelValueString));
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

            // TODO: make this configurable by user!
            // Sun degrees
            double sunriseSunDegrees = 15;
            double sunsetSunDegrees = 5;

            Calendar calendarNow = Calendar.getInstance();
            Calendar calendarSunrise = SunriseSunsetCalculator.getSunrise(mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude(), TimeZone.getDefault(), Calendar.getInstance(), sunriseSunDegrees);
            Calendar calendarSunset = SunriseSunsetCalculator.getSunset(mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude(), TimeZone.getDefault(), Calendar.getInstance(), sunsetSunDegrees);

            if ((calendarSunrise != null) && (calendarSunset != null)) {
                String sunrise = calendarSunrise.get(Calendar.HOUR_OF_DAY) + ":" + calendarSunrise.get(Calendar.MINUTE);
                String sunset = calendarSunset.get(Calendar.HOUR_OF_DAY) + ":" + calendarSunset.get(Calendar.MINUTE);
                Log.d(LOG_TAG, "Sunrise: " + sunrise);
                Log.d(LOG_TAG, "Sunset: " + sunset);

                // It's day time if the current time is after sunrise and before sunset
                return calendarNow.after(calendarSunrise) && calendarNow.before(calendarSunset);
            }
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
