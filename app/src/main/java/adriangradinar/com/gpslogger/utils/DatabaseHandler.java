package adriangradinar.com.gpslogger.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import adriangradinar.com.gpslogger.classes.UserLocation;

public class DatabaseHandler extends SQLiteOpenHelper {

    //the TAG
    private static final String TAG = DatabaseHandler.class.getSimpleName();

    //database version
    private static final int DATABASE_VERSION = 1;
    //database name
    private static final String DATABASE_NAME = "barter_database";

    //declaring the variable names for the table of transaction
    private static final String TBL_LOCATIONS = "tbl_locations";
    private static final String ID = "location_id";
    private static final String LAT = "location_latitude";
    private static final String LON = "location_longitude";
    private static final String ACC = "location_accuracy";
    private static final String TIMESTAMP = "location_timestamp";
    private static final String SYNCED = "synced";

    private static DatabaseHandler instance;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHandler getHelper(Context context) {
        if (instance == null)
            instance = new DatabaseHandler(context.getApplicationContext());
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //declare and create the transaction table
        String CREATE_LOCATIONS_TABLE = "CREATE TABLE IF NOT EXISTS " + TBL_LOCATIONS + "("
                + ID + " INTEGER PRIMARY KEY, " + LAT + " TEXT, " + LON + " TEXT, "
                + ACC + " REAL, " + TIMESTAMP + " TEXT, " + SYNCED + " INTEGER"
                + ")";
        db.setLocale(Locale.getDefault());
        db.execSQL(CREATE_LOCATIONS_TABLE);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        db.setLocale(Locale.getDefault());
        super.onOpen(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void addLocation(UserLocation location) {
        //insert the data into the database
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            synchronized (db) {
                //allow the database to create the values to be insert
                ContentValues values = new ContentValues();
                values.put(LAT, location.getLatitude());
                values.put(LON, location.getLongitude());
                values.put(ACC, location.getAccuracy());
                values.put(TIMESTAMP, location.getTimestamp());
                values.put(SYNCED, location.isSynced());

                db.insert(TBL_LOCATIONS, null, values);
                db.close();
                Log.w(TAG, "Location inserted into the database!");
            }
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
        }
    }

//    public void updateLocationStatus(ArrayList<UserLocation> userLocations) {
//
//        SQLiteDatabase db = this.getWritableDatabase();
//        //allow the database to create the values to be insert
//        ContentValues values = new ContentValues();
//        values.put(SYNCED, 1); //equivalent of true
//
//        for (UserLocation userLocation : userLocations) {
//            //insert the data into the database
//            db.update(TBL_LOCATIONS, values, ID + "='" + userLocation.getId() + "'", null);
//        }
//        db.close();
////        Log.e("DB", "Locations updated");
//    }

    public void removeLocation(UserLocation userLocation) {

        try {
            SQLiteDatabase db = this.getWritableDatabase();
            synchronized (db) {
                db.delete(TBL_LOCATIONS, ID + "='" + userLocation.getId() + "'", null);
                db.close();
            }
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
        }
    }

    public void removeLocations(final ArrayList<UserLocation> userLocations) {
//        Utils.checkRunningThread("Database");
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            synchronized (db) {
                for (UserLocation userLocation : userLocations) {
                    //delete the record
                    db.delete(TBL_LOCATIONS, ID + "='" + userLocation.getId() + "'", null);
                }
                Log.w(TAG, "Locations removed");
                db.close();
            }
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
        }
    }

//    public void changeLocationStatus(int id) {
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        //allow the database to create the values to be insert
//        ContentValues values = new ContentValues();
//        values.put(SYNCED, 1); //equivalent of true
//        //insert the data into the database
//        db.update(TBL_LOCATIONS, values, ID + "='" + id + "'", null);
//        db.close();
//    }

    public int countTotalLocationsNotSynced() {
        int total = 0;
        try {
            String sql = "SELECT COUNT(*) FROM " + TBL_LOCATIONS + " WHERE " + SYNCED + " = 0";
            SQLiteDatabase db = this.getWritableDatabase();
            synchronized (db) {
                Cursor cursor = db.rawQuery(sql, null);

                //save every event to the events list array
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            total = cursor.getInt(0);
                        }
                        while (cursor.moveToNext());
                    }
                } finally {
                    cursor.close();
                    db.close();
                }
            }
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
        }

        return total;
    }

//    public int countTotalLocationsInDatabase() {
//        int total = 0;
//        String sql = "SELECT COUNT(*) FROM " + TBL_LOCATIONS;
//
//        SQLiteDatabase db = this.getWritableDatabase();
//        Cursor cursor = db.rawQuery(sql, null);
//
//        //save every event to the events list array
//        try {
//            if (cursor.moveToFirst()) {
//                do {
//                    total = cursor.getInt(0);
//                }
//                while (cursor.moveToNext());
//            }
//        } finally {
//            cursor.close();
//        }
//
//        return total;
//    }

//    public UserLocation getLatestUserLocationNotUploaded() {
//        UserLocation tempLocation = null;
//        String sql = "SELECT * FROM " + TBL_LOCATIONS + " WHERE " + SYNCED + " = 0" + " ORDER BY " + ID + " ASC LIMIT 1";
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        Cursor cursor = db.rawQuery(sql, null);
//
//        //save every event to the events list array
//        try {
//            if (cursor.moveToFirst()) {
//                do {
//                    tempLocation = new UserLocation(cursor.getInt(0), cursor.getDouble(1), cursor.getDouble(2), cursor.getDouble(3), cursor.getString(4), Utils.convertIntToBoolean(cursor.getInt(5)));
//                }
//                while (cursor.moveToNext());
//            }
//        } finally {
//            cursor.close();
//        }
//
//        return tempLocation;
//    }

    public void createDummyData(int total) {
        for (int i = 0; i < total; i++) {
            UserLocation userLocation = new UserLocation(54.0129907, -2.7853801, 19.204999923706055, String.valueOf(1457687842), false);
            addLocation(userLocation);
        }
    }

    public void logLocations() {

        String sql = "SELECT * FROM " + TBL_LOCATIONS + " WHERE " + SYNCED + " = 0" + " ORDER BY " + ID;
        SQLiteDatabase db = this.getWritableDatabase();
        synchronized (db) {
            Cursor cursor = db.rawQuery(sql, null);

            //save every event to the events list array
            try {
                if (cursor.moveToFirst()) {
                    do {
                        Log.e("db", cursor.getInt(0) + " - " + cursor.getDouble(1) + " - " + cursor.getDouble(2) + " - " + cursor.getDouble(3) + " - " + cursor.getString(4) + " - " + Utils.convertIntToBoolean(cursor.getInt(5)));
                    }
                    while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
    }

    public ArrayList<UserLocation> getLocations(int total) {
        ArrayList<UserLocation> locations = new ArrayList<>();
        try {
            String sql = "SELECT * FROM " + TBL_LOCATIONS + " WHERE " + SYNCED + " = 0" + " ORDER BY " + ID + " ASC LIMIT " + total;
            SQLiteDatabase db = this.getWritableDatabase();
            synchronized (db) {
                Cursor cursor = db.rawQuery(sql, null);

                //save every event to the events list array
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            UserLocation tempLocation = new UserLocation(cursor.getInt(0), cursor.getDouble(1), cursor.getDouble(2), cursor.getDouble(3), cursor.getString(4), Utils.convertIntToBoolean(cursor.getInt(5)));
                            locations.add(tempLocation);
                        }
                        while (cursor.moveToNext());
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
        }

        return locations;
    }

//    public void removeLocation(JSONArray errors) throws JSONException {
//        SQLiteDatabase db = this.getWritableDatabase();
//        for (int i = 0; i < errors.length(); i++) {
//            db.delete(TBL_LOCATIONS, ID + "='" + errors.getJSONObject(i).getInt("id") + "'", null);
//        }
//        db.close();
//    }

//    public JSONArray getJsonLocations(int userID, int total) {
//        JSONArray locations = new JSONArray();
//
//        String sql = "SELECT * FROM " + TBL_LOCATIONS + " WHERE " + SYNCED + " = 0" + " ORDER BY " + ID + " ASC LIMIT " + total;
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        Cursor cursor = db.rawQuery(sql, null);
//
//        //save every event to the events list array
//        if (cursor.moveToFirst()) {
//            do {
//                try {
//                    JSONObject locationData = new JSONObject();
//                    locationData.put("user_id", userID);
//                    locationData.put("id", cursor.getInt(0));
//                    locationData.put("lat", cursor.getDouble(1));
//                    locationData.put("lon", cursor.getDouble(2));
//                    locationData.put("acc", cursor.getDouble(3));
//                    locationData.put("timestamp", cursor.getString(4));
//                    locations.put(locationData);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//            while (cursor.moveToNext());
//        }
//        cursor.close();
//        db.close();
//        return locations;
//    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }
}