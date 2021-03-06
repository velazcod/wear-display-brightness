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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Intent service used by the {@link ActivityRecognition#API} to receive the current {@link DetectedActivity} the user
 * is doing at the moment.
 * <p/>
 * When this {@link IntentService} starts, we fetch the {@link DetectedActivity} via {@link #onHandleIntent(Intent)},
 * determine the user's last known location using the {@link com.google.android.gms.location.FusedLocationProviderApi#getLastLocation(GoogleApiClient)},
 * calculate the user's sunrise/sunset times, and determine the proper {@link BrightnessLevel} and send it to the
 * wearable using the Data API.
 */
public class ActivityRecognitionIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Constants
    private static final String LOG_TAG = "ActivityRecognitionIS";

    // Members
    private GoogleApiClient mGoogleApiClient;
    private SharedPreferences mSharedPreferencesBrightnessLevels;

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
        Log.d(LOG_TAG, "onCreate()");

        mSharedPreferencesBrightnessLevels = getSharedPreferences(BrightnessLevelsPreferenceActivity.KEY_PREF_FILENAME,
                MODE_MULTI_PROCESS);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            if ((LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient) != null)
                    && (mPendingLevelToSend == -1)) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                // Synchronize the activity detection and the user's location before sending data to wearable
                if (mDetectedActivity != null) {
                    determineBrightnessLevelBasedOnData();
                }
            }

            // If the client is connected and we have pending data to be sent, simply send it
            if (mPendingLevelToSend != -1) {
                sendBrightnessLevelToWatch(mPendingLevelToSend);
            }
        }
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
        Log.d(LOG_TAG, "onHandleIntent()");
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
    }

    /**
     * Determine the brightness level that will be set on the watch based on the received data (Detected activity and
     * last known location.
     */
    private void determineBrightnessLevelBasedOnData() {
        Log.d(LOG_TAG, "determineBrightnessLevelBasedOnData()");

        boolean isDaytime = isDaytime();
        Log.d(LOG_TAG, "It's " + (isDaytime ? "day time" : "night time") + " time");

        String brightnessLevelPrefKey;
        String defaultValue;
        switch (mDetectedActivity.getType()) {
            case DetectedActivity.IN_VEHICLE:
                Log.d(LOG_TAG, "Vehicle");
                brightnessLevelPrefKey = isDaytime ? BrightnessLevelsPreferenceActivity.KEY_LEVEL_DRIVING :
                        BrightnessLevelsPreferenceActivity.KEY_LEVEL_NIGHT_DRIVING;
                defaultValue = isDaytime ? Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_DRIVING) :
                        Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_NIGHT_DRIVING);
                break;
            case DetectedActivity.ON_BICYCLE:
                Log.d(LOG_TAG, "On bicycle");
                brightnessLevelPrefKey = isDaytime ? BrightnessLevelsPreferenceActivity.KEY_LEVEL_ON_BICYCLE :
                        BrightnessLevelsPreferenceActivity.KEY_LEVEL_NIGHT_ON_BICYCLE;
                defaultValue = isDaytime ? Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_BICYCLE) :
                        Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_NIGHT_BICYCLE);
                break;
            case DetectedActivity.WALKING:
                Log.d(LOG_TAG, "Walking");
                brightnessLevelPrefKey = isDaytime ? BrightnessLevelsPreferenceActivity.KEY_LEVEL_WALKING :
                        BrightnessLevelsPreferenceActivity.KEY_LEVEL_NIGHT_WALKING;
                defaultValue = isDaytime ? Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_WALKING) :
                        Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_NIGHT_WALKING);
                break;
            case DetectedActivity.RUNNING:
                Log.d(LOG_TAG, "Running");
                brightnessLevelPrefKey = isDaytime ? BrightnessLevelsPreferenceActivity.KEY_LEVEL_RUNNING :
                        BrightnessLevelsPreferenceActivity.KEY_LEVEL_NIGHT_RUNNING;
                defaultValue = isDaytime ? Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_RUNNING) :
                        Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_NIGHT_RUNNING);
                break;
            case DetectedActivity.STILL:
                Log.d(LOG_TAG, "Still");
                brightnessLevelPrefKey = isDaytime ? BrightnessLevelsPreferenceActivity.KEY_LEVEL_STILL :
                        BrightnessLevelsPreferenceActivity.KEY_LEVEL_NIGHT_STILL;
                defaultValue = isDaytime ? Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_STILL) :
                        Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_NIGHT_STILL);
                break;
            case DetectedActivity.TILTING:
            case DetectedActivity.ON_FOOT:
                Log.d(LOG_TAG, "On foot");
                brightnessLevelPrefKey = isDaytime ? BrightnessLevelsPreferenceActivity.KEY_LEVEL_ON_FOOT :
                        BrightnessLevelsPreferenceActivity.KEY_LEVEL_NIGHT_ON_FOOT;
                defaultValue = isDaytime ? Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_ON_FOOT) :
                        Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_NIGHT_ON_FOOT);
                break;
            case DetectedActivity.UNKNOWN:
                Log.d(LOG_TAG, "Unknown");
                brightnessLevelPrefKey = isDaytime ? BrightnessLevelsPreferenceActivity.KEY_LEVEL_UNKNOWN :
                        BrightnessLevelsPreferenceActivity.KEY_LEVEL_NIGHT_UNKNOWN;
                defaultValue = isDaytime ? Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_UNKNOWN) :
                        Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_NIGHT_UNKNOWN);
                break;
            default:
                Log.d(LOG_TAG, "I have no idea what I'm doing");
                brightnessLevelPrefKey = isDaytime ? BrightnessLevelsPreferenceActivity.KEY_LEVEL_UNKNOWN :
                        BrightnessLevelsPreferenceActivity.KEY_LEVEL_NIGHT_UNKNOWN;
                defaultValue = isDaytime ? Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_UNKNOWN) :
                        Integer.toString(BrightnessLevelsPreferenceActivity.DEFAULT_LEVEL_NIGHT_UNKNOWN);
                break;
        }

        String brightnessLevelValueString = mSharedPreferencesBrightnessLevels.getString(brightnessLevelPrefKey,
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
