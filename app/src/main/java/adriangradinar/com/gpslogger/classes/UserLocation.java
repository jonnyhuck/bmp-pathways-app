package adriangradinar.com.gpslogger.classes;

/**
 * Created by adriangradinar on 21/04/15.
 */
public class UserLocation {

    private int id;
    private double latitude;
    private double longitude;
    private double accuracy;
    private String timestamp;
    private boolean isSynced;

    public UserLocation(double latitude, double longitude, double accuracy, String timestamp, boolean isSynced) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
        this.isSynced = isSynced;
    }

    public UserLocation(int id, double latitude, double longitude, double accuracy, String timestamp, boolean isSynced) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
        this.isSynced = isSynced;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setIsSynced(boolean isSynced) {
        this.isSynced = isSynced;
    }
}
