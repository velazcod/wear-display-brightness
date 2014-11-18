package com.danvelazco.wear.displaybrightness.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class ScreenMonitorService extends Service implements SensorEventListener {
    public static final String SOURCE_IS_SCREEN = "SOURCE_IS_SCREEN";
    protected static final String AMBIENT_LIGHT_VAL = "AMBIENT_LIGHT_VAL";

    private SensorManager mSensorManager;
    private Sensor mLight;
    private float lastVal;
    private BroadcastReceiver screenReceiver;


    public ScreenMonitorService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        Log.d("ScreenMonitorService", "Started");

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        Log.d("ScreenMonitorService", "Sensor's maximum range is: " + mLight.getMaximumRange());

        if (mLight==null){
            //TODO disable this function
        }

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("ScreenReceiver", "Received screen event " + intent.getAction());
                if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
                    //On API>=20 SCREEN_ON correlates with interactive, so we need to check if screen is actually on
                    if (Build.VERSION.SDK_INT >= 20) {
                        if (((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getState() == Display.STATE_OFF) {
                            Log.d("ScreenReceiver", "Just wake, not screen on" + intent.getAction());
                            return;
                        }
                    }

                    lastVal = -1;
                    mSensorManager.registerListener(ScreenMonitorService.this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
                }else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                    mSensorManager.unregisterListener(ScreenMonitorService.this);

                    //Only push the data on screen off and only if we have sensor data
                    if (lastVal!=-1){
                        Intent activityRecognitionIntent = new Intent(context, ActivityRecognitionIntentService.class);
                        activityRecognitionIntent.putExtra(SOURCE_IS_SCREEN, true);
                        activityRecognitionIntent.putExtra(AMBIENT_LIGHT_VAL, lastVal);
                        context.startService(activityRecognitionIntent);
                    }
                }
            }
        };
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, screenStateFilter);

        return super.onStartCommand(intent, flags, startID);
    }

    @Override
    public void onDestroy() {
        Log.d("ScreenMonitorService", "Stopped");
        if (mSensorManager!=null)
            mSensorManager.unregisterListener(this);
        if (screenReceiver!=null)
            unregisterReceiver(screenReceiver);
        super.onDestroy();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT){
            lastVal = event.values[0];
            //Log.d("ScreenMonitorService", "Changed "+lastVal);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Do nothing
    }
}
