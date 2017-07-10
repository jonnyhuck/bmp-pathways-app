package adriangradinar.com.gpslogger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import adriangradinar.com.gpslogger.R;

/**
 * Created by adriangradinar on 09/03/15.
 */
public class RestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

            //since we restarted the phone, we need to clean the old variable so that we can re-enable the button
            (context.getSharedPreferences(context.getString(R.string.app_name), 0)).edit().putBoolean("serviceIsRunning", true).apply();
            (context.getSharedPreferences(context.getString(R.string.app_name), 0)).edit().putLong("alarmTime", 0).apply();

            Intent service = new Intent(context, LocationService.class);
            context.startService(service);
        }

//        if("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())){
//            Log.w("TEST", "Phone is shutting down");
//        }
    }
}
