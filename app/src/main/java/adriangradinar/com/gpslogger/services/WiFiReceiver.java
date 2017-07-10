package adriangradinar.com.gpslogger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import adriangradinar.com.gpslogger.utils.ThreadManager;
import adriangradinar.com.gpslogger.utils.Utils;

public class WiFiReceiver extends BroadcastReceiver {

    private static final String TAG = WiFiReceiver.class.getSimpleName();

    public WiFiReceiver() {
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI)
            //check if we are also online
            ThreadManager.runInBackgroundThenUi(new Runnable() {
                @Override
                public void run() {
                    // wait for that tasty WiFi
                    while (!Utils.isOnline()) {
                        try {
                            Log.d(TAG, "Have Wifi Connection but not online");
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Have Wifi Connection and online");
                    Log.e(TAG, "Upload progress: " + UploadIntentService.isUploadInProgress());
                    if (!UploadIntentService.isUploadInProgress()) {
                        Log.e(TAG, "UploadIntentService is not uploading any data");
                        context.startService(new Intent(context.getApplicationContext(), UploadIntentService.class));
                    }
//                context.startService(new Intent(context.getApplicationContext(), LocationService.class).putExtra("wifi_status", true));
//                if(Utils.isMyServiceRunning(context.getApplicationContext(), LocationService.class))
//                    context.startService(new Intent(context.getApplicationContext(), LocationService.class).putExtra("wifi_status", true));
//                else
//                    Log.e(TAG, "wifi is on, but the location service is stopped, so do nothing");
                }
            });

//                while (!Utils.isOnline()) {
//                    Utils.checkRunningThread("blah");
//                    Log.d(TAG, "Have Wifi Connection but not online");
//                    Thread.sleep(1000);
//                }

            //function returned true
            //send notification to the active service
//                Log.d(TAG, "Have Wifi Connection and online");
//                Log.e(TAG, "Upload progress: " + UploadIntentService.isUploadInProgress());
//                if (!UploadIntentService.isUploadInProgress()) {
//                    Log.e(TAG, "UploadIntentService is not uploading any data");
//                    context.startService(new Intent(context.getApplicationContext(), UploadIntentService.class));
//                }
////                context.startService(new Intent(context.getApplicationContext(), LocationService.class).putExtra("wifi_status", true));
////                if(Utils.isMyServiceRunning(context.getApplicationContext(), LocationService.class))
////                    context.startService(new Intent(context.getApplicationContext(), LocationService.class).putExtra("wifi_status", true));
////                else
////                    Log.e(TAG, "wifi is on, but the location service is stopped, so do nothing");
        else {
            Log.d(TAG, "Don't have Wifi Connection");
            Log.e(TAG, "Upload progress: " + UploadIntentService.isUploadInProgress());
//            if(Utils.isMyServiceRunning(context.getApplicationContext(), LocationService.class))
//                context.startService(new Intent(context, LocationService.class).putExtra("wifi_status", false));
//            else
//                Log.e(TAG, "wifi if OFF, but the location service is also off, so don't do anything");
        }
    }
}
