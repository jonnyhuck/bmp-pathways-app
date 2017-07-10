package adriangradinar.com.gpslogger.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import adriangradinar.com.gpslogger.MainActivity;
import adriangradinar.com.gpslogger.R;
import adriangradinar.com.gpslogger.classes.UserLocation;
import adriangradinar.com.gpslogger.utils.CustomExceptionHandler;
import adriangradinar.com.gpslogger.utils.DatabaseHandler;
import adriangradinar.com.gpslogger.utils.Utils;

/**
 * Created by adriangradinar on 09/03/15.
 */
public class LocationService extends Service implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

    public static final String NOTIFICATION = "adriangradinar.com.gpslogger.services.LocationService";
    public static final int ALARM_REQUEST = 2;
    public static final int GPS_OFF = 3;

    private final static int FASTEST_TIME_INTERVAL = 500;
    private final static int UPDATE_TIME_INTERVAL = 2000;

    private static boolean isGpsListenerSet = false;
    private final String TAG = LocationService.class.getSimpleName();
    private String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/GpsLogger";

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation = null;
    private LocationRequest locationRequest;
    private DatabaseHandler db;
    private SharedPreferences sharedPreferences;
    private int userID = 0;

    private LocationManager locationManager;

    private SaveLocationToDatabase saveLocationToDatabase;
    private UploadLocationToServer uploadLocationToServer;

    @Override
    public void onCreate() {
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(fullPath));
        }

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!isGpsListenerSet) {
                locationManager.addGpsStatusListener(new GpsStatus.Listener() {
                    @Override
                    public void onGpsStatusChanged(int event) {
                        if (event == GpsStatus.GPS_EVENT_STOPPED) {
                            if (!((LocationManager) LocationService.this.getApplicationContext().getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                Utils.createNotification(LocationService.this.getApplicationContext(), "Location was deactivated!");
                                sendBroadcast(new Intent(NOTIFICATION).putExtra("result", GPS_OFF));
                            }
                        }
                    }
                });
                isGpsListenerSet = true;
            }
        }

        buildGoogleApiClient();
        if (!Utils.checkIfGpsIsOn(this)) {
            Utils.createNotification(LocationService.this.getApplicationContext(), "Location is deactivated!");
        }

        locationRequest = new LocationRequest();
        db = DatabaseHandler.getHelper(LocationService.this.getApplicationContext());
        sharedPreferences = this.getSharedPreferences(getString(R.string.app_name), 0);
    }

    //---- Location data
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Suspended!");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection failed!");
    }

    protected void createLocationRequest() {
        locationRequest.setFastestInterval(FASTEST_TIME_INTERVAL);
        locationRequest.setInterval(UPDATE_TIME_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (sharedPreferences.getBoolean(getString(R.string.service_is_running), true)) {
            //connect to the location client
            startUpdating();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
//        Log.e(TAG, "location changed " + location.getLatitude() + " - " + location.getLongitude());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //if the service is running
        if (sharedPreferences.getBoolean(getString(R.string.service_is_running), true)) {

            //connect the google client in case it was disconnected
            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }

            //this handles the explicit sending of intents
            if (intent != null) {

                //handles the wifi change status
//                if(intent.hasExtra("wifi_status")) {
//                    boolean wifiStatus = intent.getExtras().getBoolean("wifi_status");
//                    if (wifiStatus) {
//                        //if there's WI-FI, send a request to upload the current data if the upload service is not already running
//                        //it shouldn't but in case wi-fi dropped, possibly this is the best case scenario
//
//                        Log.e(TAG, "wifi is ON");
//                        if(!UploadIntentService.isUploadInProgress()){
//                            Log.e(TAG, "UploadIntentService is not uploading any data");
//                            startService(new Intent(LocationService.this, UploadIntentService.class));
//                        }
//                        else{
//                            Log.e(TAG, "UploadIntentService is UPLOADING, we don't want to duplicate things!");
//                        }
//
////                        if (!Utils.isMyServiceRunning(getApplicationContext(), UploadIntentService.class)) {
////                            Log.e(TAG, "WI-FI on, start service");
////                            startService(new Intent(LocationService.this, UploadIntentService.class));
////                        }
////                        else{
////                            Log.e(TAG, "WI-FI on, service already running");
////                        }
//                    } else {
//                        //wi-fi dropped, start recoding the location
//                        Log.e(TAG, "wifi dropped but we're still waiting for volley to timeout");
////                        if(saveLocationToDatabase == null) {
////                            saveLocationToDatabase = new SaveLocationToDatabase(1);
////                            saveLocationToDatabase.execute();
////                        }
////                        saveLocationToDatabase.setRunning(true);
//                    }
//                }

                //handles the request for stopping the save of the location
                if (intent.hasExtra(UploadIntentService.UPLOAD_SERVER_RESPONSE)) {
                    Log.w(TAG, "Request for stopping the save for the location...");
                    if (saveLocationToDatabase != null) {
                        saveLocationToDatabase.setRunning(false);
                    }
                }

                //handles the request for when the server has finished the upload
                if (intent.hasExtra(UploadIntentService.UPLOAD_SERVER_FINISHED)) {// && sharedPreferences.getBoolean(getString(R.string.service_is_running), true)) {
                    Log.w(TAG, "Finished. Request for saving the location...");
                    if (saveLocationToDatabase == null) {
                        saveLocationToDatabase = new SaveLocationToDatabase(1);
                        saveLocationToDatabase.execute();
                    }
                    saveLocationToDatabase.setRunning(true);
                }

                //handles the request for when the upload process was interrupted
                if (intent.hasExtra(UploadIntentService.UPLOAD_SERVER_INTERRUPTED)) {
                    Log.w(TAG, "Interrupted. Request for saving the location...");
                    if (saveLocationToDatabase == null) {
                        saveLocationToDatabase = new SaveLocationToDatabase(1);
                        saveLocationToDatabase.execute();
                    }
                    saveLocationToDatabase.setRunning(true);
                }

                //handles the start requests from the main activity
                if (intent.hasExtra(MainActivity.START_FROM_MAIN)) {

                    Log.e(TAG, "start from main requested");

                    if (saveLocationToDatabase == null) {
                        saveLocationToDatabase = new SaveLocationToDatabase(1);
                        saveLocationToDatabase.execute();
                    }
                    saveLocationToDatabase.setRunning(true);

                    if (uploadLocationToServer == null) {
                        uploadLocationToServer = new UploadLocationToServer(1);
                        uploadLocationToServer.execute();
                    }
                }

                //handles the start requests from the main activity
                if (intent.hasExtra(AlarmReceiver.START_FROM_ALARM)) {

                    Log.e(TAG, "start from alarm requested");

                    if (saveLocationToDatabase == null) {
                        saveLocationToDatabase = new SaveLocationToDatabase(1);
                        saveLocationToDatabase.execute();
                    }
                    saveLocationToDatabase.setRunning(true);

                    if (uploadLocationToServer == null) {
                        uploadLocationToServer = new UploadLocationToServer(1);
                        uploadLocationToServer.execute();
                    }
                }
            } else {

                Log.e(TAG, "intent null");

                //intent is null due to the START_STICKY
                if (saveLocationToDatabase == null) {
                    saveLocationToDatabase = new SaveLocationToDatabase(1);
                    saveLocationToDatabase.execute();
                }
                saveLocationToDatabase.setRunning(true);

                if (uploadLocationToServer == null) {
                    uploadLocationToServer = new UploadLocationToServer(1);
                    uploadLocationToServer.execute();
                }
            }
        } else {
            Log.e(TAG, "our records show that this service is stopped, so we'll kill it!");
            //our records show that this service is stopped, so we'll kill it!
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "Service destroyed");

        stopUpdating();
        super.onDestroy();
    }

    private void startUpdating() {
        //request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    private void stopUpdating() {
        //stop the updates before the location request is cancelled
        if (saveLocationToDatabase != null)
            saveLocationToDatabase.setRunning(false);

        //remove the location listener
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Class to handle the period saving of a given location, defined by the internal variable
     * Can be stopped by changing the status of the boolean isRunning variable
     */
    private class SaveLocationToDatabase extends ScheduledThreadPoolExecutor implements Runnable {

        private final static int SAVE_TIME_INTERVAL = 4;

        private boolean isRunning;

        public SaveLocationToDatabase(int corePoolSize) {
            super(corePoolSize);
        }

        @Override
        public void run() {
            if (isRunning) {
                if (mLastLocation != null) {
                    Long tsLong = System.currentTimeMillis() / 1000;
                    db.addLocation(new UserLocation(userID, mLastLocation.getLatitude(), mLastLocation.getLongitude(), mLastLocation.getAccuracy(), tsLong.toString(), false));
                }
            }
        }

        public void setRunning(boolean running) {
            Log.e(TAG, "running set to: " + running);
            isRunning = running;
        }

        private void execute() {
            scheduleAtFixedRate(this, 0, SAVE_TIME_INTERVAL, TimeUnit.SECONDS);
        }
    }

    /**
     * Class to handle the period execution of the upload defined by the internal variable
     * Currently, we don't really stop this, even if the user stops the saving process
     */
    private class UploadLocationToServer extends ScheduledThreadPoolExecutor implements Runnable {

        private final static int UPLOAD_TIME_INTERVAL = 2;

        public UploadLocationToServer(int corePoolSize) {
            super(corePoolSize);
        }

        @Override
        public void run() {
            if (!UploadIntentService.isUploadInProgress()) {
                Log.e(TAG, "Upload called!");
                startService(new Intent(LocationService.this, UploadIntentService.class));
            } else {
                Log.e(TAG, "It looks like the upload service is performing an upload... this may be due to it actually doing an update OR one of the variables not being set correctly!");
            }
        }

        private void execute() {
            scheduleAtFixedRate(this, 0, UPLOAD_TIME_INTERVAL, TimeUnit.HOURS);
        }
    }
}