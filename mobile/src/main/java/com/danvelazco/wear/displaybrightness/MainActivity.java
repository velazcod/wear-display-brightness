package com.danvelazco.wear.displaybrightness;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.danvelazco.wear.displaybrightness.shared.BrightnessLevel;
import com.danvelazco.wear.displaybrightness.util.ActivityRecognitionHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Constants
    private static final String LOG_TAG = "MainActivity";

    // Members
    private GoogleApiClient mGoogleApiClient;
    private TextView mTvDebugInfo = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Schedule the activity detection updates
        ActivityRecognitionHelper activityRecognitionHelper = new ActivityRecognitionHelper(this);
        activityRecognitionHelper.scheduleActivityUpdates();

        // Build the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Button btnHideActivity = (Button) findViewById(R.id.btn_hide_activity);
        btnHideActivity.setOnClickListener(this);

        // If we are in debug mode, show these icons so that we can manually set/control the brightness levels
        // on the watch from the phone, for testing purposes
        if (BuildConfig.DEBUG) {
            ViewGroup buttonsViewGroup = (ViewGroup) findViewById(R.id.layout_buttons);
            buttonsViewGroup.setVisibility(View.VISIBLE);

            mTvDebugInfo = (TextView) findViewById(R.id.tv_debug_info);
            mTvDebugInfo.setVisibility(View.VISIBLE);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String sunrise = sharedPreferences.getString("sunrise_time", null);
            String sunset = sharedPreferences.getString("sunset_time", null);
            mTvDebugInfo.setText("Sunrise: " + sunrise + " - Sunset: " + sunset);

            Button btnBrightnessLow = (Button) findViewById(R.id.btn_brightness_low);
            btnBrightnessLow.setOnClickListener(this);

            Button btnBrightnessMed = (Button) findViewById(R.id.btn_brightness_med);
            btnBrightnessMed.setOnClickListener(this);

            Button btnBrightnessHi = (Button) findViewById(R.id.btn_brightness_hi);
            btnBrightnessHi.setOnClickListener(this);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();

        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }

        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnected(Bundle bundle) {
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_hide_activity:
                hideAppIcon();
                finish();
                break;
            case R.id.btn_brightness_low:
                sendBrightnessLevelToWatch(BrightnessLevel.LOWEST);
                break;
            case R.id.btn_brightness_med:
                sendBrightnessLevelToWatch(BrightnessLevel.MEDIUM);
                break;
            case R.id.btn_brightness_hi:
                sendBrightnessLevelToWatch(BrightnessLevel.HIGHEST);
                break;
        }
    }

    /**
     * Use the {@link PackageManager} to disable this activity's component which will in turn hide the icon from the
     * launcher
     */
    private void hideAppIcon() {
        PackageManager pm = getPackageManager();
        if (pm != null) {
            pm.setComponentEnabledSetting(new ComponentName(this, MainActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
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
    }

}
