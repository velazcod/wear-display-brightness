package com.danvelazco.wear.displaybrightness.util;

import android.content.Context;
import android.content.Intent;

public class AmbientModeUtil {

    public static boolean isAmbientModeEnabled(Context context) {
        boolean isAmbientDisabled = SystemPropertiesProxy.getBoolean(context, "persist.sys.disable_ambient", false);
        return !isAmbientDisabled;
    }

    public static void setAmbientMode(Context context, boolean ambientEnabled) {
        SystemPropertiesProxy.set(context, "persist.sys.disable_ambient", ambientEnabled);

        //DeveloperOptionsActivity.pokeSystemProperties();
        //(new SystemPropPoker()).execute();

        Intent intent = new Intent("com.google.android.clockwork.settings.TOGGLE_AMBIENT");
        intent.putExtra("disabled", !ambientEnabled);
        context.sendBroadcast(intent);
    }

//    static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
//        @Override
//        protected Void doInBackground(Void... params) {
//            String[] services;
//            try {
//                services = ServiceManager.listServices();
//            } catch (RemoteException e) {
//                return null;
//            }
//            for (String service : services) {
//                IBinder obj = ServiceManager.checkService(service);
//                if (obj != null) {
//                    Parcel data = Parcel.obtain();
//                    try {
//                        obj.transact(IBinder.SYSPROPS_TRANSACTION, data, null, 0);
//                    } catch (RemoteException e) {
//                    } catch (Exception e) {
//                        Log.i("DevSettings", "Somone wrote a bad service '" + service
//                                + "' that doesn't like to be poked: " + e);
//                    }
//                    data.recycle();
//                }
//            }
//            return null;
//        }
//    }

}
