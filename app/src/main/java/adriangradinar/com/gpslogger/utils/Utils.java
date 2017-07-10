package adriangradinar.com.gpslogger.utils;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import adriangradinar.com.gpslogger.MainActivity;
import adriangradinar.com.gpslogger.R;

public class Utils {

    public static void checkRunningThread(String TAG) {
        Log.e(TAG, "Running in thread: " + Thread.currentThread().toString());
    }

    public static boolean checkConnectivity(final Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE);
        } catch (Exception e) {
            //temporarily print out the error connection message
            System.out.println("CheckConnectivity Exception: " + e.getMessage());
        }
        return false;
    }

    static boolean convertIntToBoolean(int integerValue) {
        //boolean convertedValue = integerValue > 0 ? true : false;
        return (integerValue != 0);
    }

    public static void createNotification(Context context, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

        // Build notification
        // Actions are just fake
        Notification noti = new Notification.Builder(context)
                .setAutoCancel(true)
                .setContentTitle("Belfast Tracker")
                .setContentText(message)
                .setSmallIcon(R.drawable.notification)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setContentIntent(pIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // hide the notification after its selected
        noti.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, noti);
    }

    public static boolean checkIfGpsIsOn(Context context) {
        return ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

//    public static boolean isConnected(Context context) {
//        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
//        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
//            return true;
//        } else {
//            return false;
//        }
//    }

    public static boolean isWifiON(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && Utils.isOnline();
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.e(context.getPackageName(), "Yes, service " + serviceClass.getName() + " is running!");
                return true;
            }
        }
        Log.e(context.getPackageName(), "Yes, service " + serviceClass.getName() + " is NOT running!");
        return false;
    }

    public static void writeToFile(String fullPath, String fileName, String msg, String TAG) {
        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileOutputStream fos;
            File myFile = new File(fullPath, fileName);
            if (!myFile.exists())
                myFile.createNewFile();
            byte[] data = msg.getBytes();
            try {
                fos = new FileOutputStream(myFile, true);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Couldn't find the file");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "An I/O Error occurred");
            e.printStackTrace();
        }
    }
}


