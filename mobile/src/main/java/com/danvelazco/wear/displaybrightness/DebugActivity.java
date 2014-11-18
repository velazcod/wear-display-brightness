package com.danvelazco.wear.displaybrightness;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.danvelazco.wear.displaybrightness.receiver.ScreenReceiver;
import com.danvelazco.wear.displaybrightness.shared.BrightnessLevel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * @author Daniel Velazco <velazcod@gmail.com>
 * @since 9/15/14
 */
public class DebugActivity extends Activity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GooglePlayServicesClient.ConnectionCallbacks {

    // Constants
    private static final String LOG_TAG = "DebugActivity";

    // Members
    private GoogleApiClient mGoogleApiClient;

    // Views
    private Button mBtnLowest;
    private Button mBtnMediumLow;
    private Button mBtnMedium;
    private Button mBtnMediumHigh;
    private Button mBtnHighest;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE
                    | ActionBar.DISPLAY_HOME_AS_UP
                    | ActionBar.DISPLAY_SHOW_HOME);
            actionBar.show();
            actionBar.setTitle(getString(R.string.lbl_debug));
        }

        mBtnLowest = (Button) findViewById(R.id.btn_level_lowest);
        mBtnLowest.setOnClickListener(this);

        mBtnMediumLow = (Button) findViewById(R.id.btn_level_medium_low);
        mBtnMediumLow.setOnClickListener(this);

        mBtnMedium = (Button) findViewById(R.id.btn_level_medium);
        mBtnMedium.setOnClickListener(this);

        mBtnMediumHigh = (Button) findViewById(R.id.btn_level_medium_high);
        mBtnMediumHigh.setOnClickListener(this);

        mBtnHighest = (Button) findViewById(R.id.btn_level_highest);
        mBtnHighest.setOnClickListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        BroadcastReceiver screenReceiver = new ScreenReceiver();
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, screenStateFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_level_lowest:
                sendBrightnessLevelToWatch(BrightnessLevel.LOWEST);
                break;
            case R.id.btn_level_medium_low:
                sendBrightnessLevelToWatch(BrightnessLevel.MEDIUM_LOW);
                break;
            case R.id.btn_level_medium:
                sendBrightnessLevelToWatch(BrightnessLevel.MEDIUM);
                break;
            case R.id.btn_level_medium_high:
                sendBrightnessLevelToWatch(BrightnessLevel.MEDIUM_HIGH);
                break;
            case R.id.btn_level_highest:
                sendBrightnessLevelToWatch(BrightnessLevel.HIGHEST);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mBtnLowest.setEnabled(true);
            mBtnMediumLow.setEnabled(true);
            mBtnMedium.setEnabled(true);
            mBtnMediumHigh.setEnabled(true);
            mBtnHighest.setEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnected() {
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            mBtnLowest.setEnabled(false);
            mBtnMediumLow.setEnabled(false);
            mBtnMedium.setEnabled(false);
            mBtnMediumHigh.setEnabled(false);
            mBtnHighest.setEnabled(false);
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
