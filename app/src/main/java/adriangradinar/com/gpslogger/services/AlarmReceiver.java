package adriangradinar.com.gpslogger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import adriangradinar.com.gpslogger.R;
import adriangradinar.com.gpslogger.utils.Utils;

/**
 * Created by adriangradinar on 24/08/15.
 */
public class AlarmReceiver extends BroadcastReceiver {

    public static final String START_FROM_ALARM = "start service from alarm";
    private static final String TAG = BroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.e(TAG, "Restarted!");
//        Log.e(TAG, "hmm... wtf!?!");

        //save the state of the service
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), 0);
        sharedPreferences.edit().putLong("alarmTime", 0).apply();

        //re-start the service
        context.startService(new Intent(context, LocationService.class).putExtra(AlarmReceiver.START_FROM_ALARM, true));
        if (!Utils.checkIfGpsIsOn(context))
            Utils.createNotification(context.getApplicationContext(), "Please ensure Location is enabled and set to High Accuracy!");
        else {
            if (!sharedPreferences.getBoolean(context.getString(R.string.service_is_running), true)) {
//                Log.e(TAG, "in the false");
                //let's set it to true
                sharedPreferences.edit().putBoolean(context.getString(R.string.service_is_running), true).apply();
                //send the notification since the system cancelled the alarm
                Utils.createNotification(context, "The application has started tracking your location!");
                //no need to send any notifications
            } else {
//                Log.e(TAG, "in the true");
                // no need to send any notifications
            }
        }

        publishResults(context, LocationService.ALARM_REQUEST);
    }

    private void publishResults(Context context, int result) {
        Intent intent = new Intent(LocationService.NOTIFICATION);
        intent.putExtra("result", result);
        context.sendBroadcast(intent);
    }
}
