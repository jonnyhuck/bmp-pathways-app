package adriangradinar.com.gpslogger.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import adriangradinar.com.gpslogger.ApplicationController;
import adriangradinar.com.gpslogger.R;
import adriangradinar.com.gpslogger.classes.UserLocation;
import adriangradinar.com.gpslogger.utils.DatabaseHandler;
import adriangradinar.com.gpslogger.utils.Utils;

/**
 *
 */
public class UploadIntentService extends IntentService {

    public static final String NOTIFICATION = "adriangradinar.com.gpslogger.services.UploadIntentService";
    public static final int USER_ID_REQUEST = 1;

    public final static String UPLOAD_SERVER_FINISHED = "upload server finished";
    public final static String UPLOAD_SERVER_BATCH_FINISHED = "upload server batch finished";
    public final static String UPLOAD_SERVER_RESPONSE = "upload server response";
    public final static String UPLOAD_SERVER_INTERRUPTED = "upload server interrupted";

    private static final String TAG = UploadIntentService.class.getSimpleName();
    private static final String GENERATE_ID = "generateId.php";
    private static final String LOG_GPS = "logGpsArray.php";
    private static final int UPLOAD_ENTRIES_LIMIT = 2000;
    private static boolean uploadInProgress = false;
    private DatabaseHandler db;
    private SharedPreferences sharedPreferences;
    private ArrayList<UserLocation> userLocations = new ArrayList<>();
    private Handler connectionHandler;
    public UploadIntentService() {
        super("UploadIntentService");
    }

    public static boolean isUploadInProgress() {
        return uploadInProgress;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connectionHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            sharedPreferences = this.getSharedPreferences(getString(R.string.app_name), 0);
            db = new DatabaseHandler(getApplicationContext());

            Log.e(TAG, "we got called again...");

            //check if we have a user ID
            int userID = sharedPreferences.getInt("id", 0);
            if (userID == 0) {
                //since we have no used ID, let's fetch one from the server
                getUserId();
            }
            //we already have a userID, let's just upload the data
            else {
                uploadData(userID);
            }
        }
    }

    /**
     * Creates a connection to the server and retrieves a UserID which is saved to the persisting memory for later use
     */
    private void getUserId() {
        if (Utils.checkConnectivity(getApplicationContext()))
            try {
                JsonObjectRequest request = new JsonObjectRequest(getString(R.string.base_url) + GENERATE_ID, new JSONObject().put("id", 0), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("result")) {
                                //get the ID
                                final int userID = response.getInt("user_id");
                                //save the ID to the pref :)
                                sharedPreferences.edit().putInt("id", userID).apply();
                                //send the newly acquired ID to the main activity
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getApplicationContext().sendBroadcast(new Intent(NOTIFICATION).putExtra("result", USER_ID_REQUEST).putExtra("ID", userID));
                                        uploadData(userID);
                                    }
                                }).run();
                            } else {
                                Log.e(TAG, response.getBoolean("result") + " is not valid!");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "error on receiving the userID");
                        error.printStackTrace();
                    }
                });

                //add the request to the queue
                ApplicationController.getInstance().addToRequestQueue(request);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        else {
            connectionHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "In order to communicate with the server, you need a valid internet connection, please make sure you are connected to either a Wi-Fi hotspot or have data enabled from your Mobile Network Provider", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void uploadData(final int userID) {
        Log.e(TAG, "uploadData function called - upload progress: " + uploadInProgress);
        if (Utils.isWifiON(getApplicationContext()) && Utils.isOnline()) {

            uploadInProgress = true;

            int howMany = db.countTotalLocationsNotSynced();
            Log.w(TAG, "Total entries not synced: " + howMany);
            if (howMany > UPLOAD_ENTRIES_LIMIT) {
                howMany = UPLOAD_ENTRIES_LIMIT;
            }

            userLocations = db.getLocations(howMany);

            if (userLocations.size() != 0) {

                //we'll be stopping the location writing to the database by sending a request to our service
                startService(new Intent(UploadIntentService.this, LocationService.class).putExtra(UPLOAD_SERVER_RESPONSE, true));

                //create the JSON
                JSONArray userData = new JSONArray();

                //parse the data into the JSON
                for (UserLocation userLocation : userLocations) {
                    try {
                        JSONObject locationData = new JSONObject();
                        locationData.put("user_id", userID);
                        locationData.put("id", userLocation.getId());
                        locationData.put("lat", userLocation.getLatitude());
                        locationData.put("lon", userLocation.getLongitude());
                        locationData.put("acc", userLocation.getAccuracy());
                        locationData.put("timestamp", userLocation.getTimestamp());
                        userData.put(locationData);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                Log.w(TAG, "Selected locations: " + userLocations.size() + "; Json locations: " + userData.length());

                JsonArrayRequest request = new JsonArrayRequest(getString(R.string.base_url) + LOG_GPS, userData, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            JSONObject allData = response.getJSONObject(0);
                            if (!allData.getBoolean("received")) {
                                JSONArray errors = allData.getJSONArray("notEntered");
                                boolean found = false;
                                Log.w(TAG, "Start searching for IDs in the JsonArray we received from the server!");
                                for (UserLocation userLocation : userLocations) {
                                    for (int i = 0; i < errors.length(); i++) {
                                        if (userLocation.getId() == errors.getJSONObject(i).getInt("id")) {
                                            found = true;
                                        }
                                    }
                                    if (!found) {
                                        db.removeLocation(userLocation);
                                    } else {
                                        found = false;
                                    }
                                }
                                Log.w(TAG, "Finished searching!");

                                uploadInProgress = false;
                            } else {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //all data was successfully updated - delete records
                                        db.removeLocations(userLocations);
                                        Log.w(TAG, "Upload performed successfully! This is called every time a batch has been uploaded successfully!");
                                        //we most probably have more locations to upload - recursive call!
                                        if (!Utils.isWifiON(getApplicationContext())) {
                                            //darn we lost the Wi-Fi, so let's send an intent to start saving locations
                                            uploadInProgress = false;
                                            startService(new Intent(UploadIntentService.this, LocationService.class).putExtra(UPLOAD_SERVER_FINISHED, true));
                                        } else
                                            uploadData(userID);
                                    }
                                }).start();
                            }
                        } catch (JSONException e) {
                            uploadInProgress = false;
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        uploadInProgress = false;
                        startService(new Intent(UploadIntentService.this, LocationService.class).putExtra(UPLOAD_SERVER_INTERRUPTED, true));
                        error.printStackTrace();
                    }
                });

                // add the request object to the queue to be executed
                ApplicationController.getInstance().addToRequestQueue(request);
            } else {
                Log.w(TAG, "There are no more entries in the database!");
                uploadInProgress = false;
                startService(new Intent(UploadIntentService.this, LocationService.class).putExtra(UPLOAD_SERVER_FINISHED, true));
            }
        } else {
            Log.w(TAG, "Not met the network requirements for upload!");
        }
    }
}
