package adriangradinar.com.gpslogger;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import adriangradinar.com.gpslogger.services.AlarmReceiver;
import adriangradinar.com.gpslogger.services.LocationService;
import adriangradinar.com.gpslogger.services.UploadIntentService;
import adriangradinar.com.gpslogger.utils.Utils;


public class MainActivity extends AppCompatActivity {

    public static final String START_FROM_MAIN = "start service from main";
    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int RESULT_GSP_ON = 100;
    private SharedPreferences sharedPreferences;
    private AlertDialog dialog = null;
    private MenuItem stopItem, resumeItem;
    private TextView notificationText, userIdText;
    private SimpleDateFormat sdf;
    private boolean isGpsON = false;
    private Button button;

    private PendingIntent pendingIntent;
    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                int result = bundle.getInt("result");
                switch (result) {
                    case LocationService.ALARM_REQUEST:
                        stopItem.setVisible(true);
                        resumeItem.setVisible(false);
                        setTextToTextView(notificationText, getString(R.string.tracking_on));
                        break;
                    case LocationService.GPS_OFF:
                        setTextToTextView(notificationText, getString(R.string.tracking_off));
                        break;
                }
            }
        }
    };

    private BroadcastReceiver uploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                int result = bundle.getInt("result");
                switch (result) {
                    case UploadIntentService.USER_ID_REQUEST:
                        Log.e(TAG, "userID: " + bundle.getInt("ID"));
                        setTextToTextView(userIdText, String.valueOf(bundle.getInt("ID")));
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        button = (Button) findViewById(R.id.button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Log.e(TAG, "pressed!");
//
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
////                        db.createDummyData(302400);
//                        Log.e(TAG, "entries: " + db.countTotalLocationsNotSynced());
//                    }
//                }).start();
//            }
//        });

        userIdText = (TextView) findViewById(R.id.userIdText);
        notificationText = (TextView) findViewById(R.id.notification);
        sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

        sharedPreferences = this.getSharedPreferences(getString(R.string.app_name), 0);
        if (!sharedPreferences.contains(getString(R.string.service_is_running))) {
//            Log.e(TAG, "Let's create the service variable and set it to true...");
            sharedPreferences.edit().putBoolean(getString(R.string.service_is_running), true).apply();
        }

        int userId = sharedPreferences.getInt("id", 0);
        if (userId == 0) {
            setTextToTextView(userIdText, "waiting...");
        } else {
            setTextToTextView(userIdText, String.valueOf(userId));
        }

        //start the service
        if (!Utils.isMyServiceRunning(getApplicationContext(), LocationService.class))
            startService(new Intent(MainActivity.this, LocationService.class).putExtra(START_FROM_MAIN, true));
        else
            Log.e(TAG, "Main Activity - onCreate - service is already running");
    }

    private void navigateToLocationSettings() {
        Toast.makeText(getApplicationContext(), "Please enable Location and set the Mode to High Accuracy!", Toast.LENGTH_LONG).show();
        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), RESULT_GSP_ON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_GSP_ON && !Utils.checkIfGpsIsOn(this))
            navigateToLocationSettings();

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void stopRecordingLocation() {
        final View dialogLayout = getLayoutInflater().inflate(R.layout.confirm_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogLayout);

        (dialogLayout.findViewById(R.id.cancelBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        (dialogLayout.findViewById(R.id.okBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] time = ((Spinner) dialogLayout.findViewById(R.id.spinner2)).getSelectedItem().toString().split(" ");
                scheduleAlarm(Integer.parseInt(time[0]));
//                stopItem.setVisible(false);
                dialog.cancel();
            }
        });

        this.dialog = builder.create();
        this.dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        stopItem = menu.findItem(R.id.stop);
        resumeItem = menu.findItem(R.id.resume);

        //if the service is still off, disabled the button
        if (sharedPreferences.getBoolean(getString(R.string.service_is_running), true)) {
            menu.findItem(R.id.stop).setVisible(true);
            menu.findItem(R.id.resume).setVisible(false);
        } else {
            menu.findItem(R.id.stop).setVisible(false);
            menu.findItem(R.id.resume).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stop:
                stopRecordingLocation();
                break;
            case R.id.resume:
                cancelAlarm();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void scheduleAlarm(int hours) {
        //add the time to the current time
        Long time = new GregorianCalendar().getTimeInMillis() + (hours * 60 * 60 * 1000); //has to be hours * 60 * 60 * 1000
//        Long time = new GregorianCalendar().getTimeInMillis() + (1 * 60 * 1000); // 1min
        Intent intentAlarm = new Intent(this, AlarmReceiver.class);
        intentAlarm.setAction("adriangradinar.com.gpslogger.ACTION");
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //set the alarm for particular time
        pendingIntent = PendingIntent.getBroadcast(this, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
        //save the state of the service to memory
        sharedPreferences.edit().putBoolean(getString(R.string.service_is_running), false).apply();
        //stop the service
        stopService(new Intent(this, LocationService.class));

        //update the buttons
        stopItem.setVisible(false);
        resumeItem.setVisible(true);

        setTextToTextView(notificationText, getString(R.string.tracking_resumed) + sdf.format(time));
        sharedPreferences.edit().putLong("alarmTime", time).apply();
//        Log.w(TAG, "alarm set");
    }

    public void cancelAlarm() {
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(pendingIntent);
        sharedPreferences.edit().putBoolean(getString(R.string.service_is_running), true).apply();
        sharedPreferences.edit().putLong("alarmTime", 0).apply();
        setTextToTextView(notificationText, getString(R.string.tracking_on));

//        Log.e("cancel alarm", "cancel alarm request sent!");

        //update the buttons
        stopItem.setVisible(true);
        resumeItem.setVisible(false);

        if (!Utils.isMyServiceRunning(getApplicationContext(), LocationService.class))
            startService(new Intent(MainActivity.this, LocationService.class).putExtra(START_FROM_MAIN, true));
        else
            Log.e(TAG, "Main Activity - cancel alarm - service is already running");
    }

    private void setTextToTextView(final TextView textView, final String message) {
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(message);
            }
        });
    }

    @Override
    protected void onResume() {
        if (!Utils.checkIfGpsIsOn(this)) {
            setTextToTextView(notificationText, getString(R.string.tracking_off));
            navigateToLocationSettings();
        } else {
            isGpsON = true;
        }

        registerReceiver(locationReceiver, new IntentFilter(LocationService.NOTIFICATION));
        registerReceiver(uploadReceiver, new IntentFilter(UploadIntentService.NOTIFICATION));

        long alarmTime = sharedPreferences.getLong("alarmTime", 0);
        if (alarmTime == 0) {
            if (isGpsON)
                setTextToTextView(notificationText, getString(R.string.tracking_on));
            if (stopItem != null) {
                stopItem.setVisible(true);
                resumeItem.setVisible(false);
            }
        } else {
            long currentTime = new GregorianCalendar().getTimeInMillis();
            if (currentTime > alarmTime) {
                setTextToTextView(notificationText, getString(R.string.tracking_on));
                if (stopItem != null) {
                    stopItem.setVisible(true);
                    resumeItem.setVisible(false);
                }
            } else {
                setTextToTextView(notificationText, getString(R.string.tracking_resumed) + String.valueOf(sdf.format(alarmTime)));
            }
        }

        //display the userID, if we have one, else just leave it as it is...
        int userID = sharedPreferences.getInt("id", 0);
        if (userID != 0)
            setTextToTextView(userIdText, String.valueOf(userID));

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationReceiver);
        unregisterReceiver(uploadReceiver);
    }
}
