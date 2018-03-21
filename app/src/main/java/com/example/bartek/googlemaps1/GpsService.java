package com.example.bartek.googlemaps1;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by bartek on 14.03.2018.
 */

public class GpsService extends Service implements AsyncTaskDatabase.AsyncResponse {
    private static final String TAG = "GpsService";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 2000;
    private static final float LOCATION_DISTANCE = 10f;
    private Intent intent;
    private double speed;
    private long time;
    private Location mLastLocation;
    private NotificationCompat.Builder nofificationBuilder;
    private NotificationManager notificationManager;
    private AsyncTaskDatabase asyncTaskDatabase;
    private User user;

    @Override
    public void getUserResponse(User user) {
        this.user = user;
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    private class LocationListener implements android.location.LocationListener {

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
            Log.d(TAG, "LocationListener: " + mLastLocation);
        }

        @Override
        public void onLocationChanged(Location location) {
            time = Calendar.getInstance().getTimeInMillis();
            Log.e(TAG, "onLocationChanged: " + location);
            if (mLastLocation.getLongitude() != 0 && mLastLocation.getLongitude() != 0)
                user.addDistance(mLastLocation.distanceTo(location));
            mLastLocation.set(location);
            if (location != null) {
                user.setEnd_time(time);
                user.addLatitude(location.getLatitude());
                user.addLongitude(location.getLongitude());
                speed = location.getSpeed();
                speed *= 3.6;
                Log.e(TAG, "onLocationChanged: " + speed );
                user.addSpeed(speed);
                asyncTaskDatabase.update(user);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        intent = new Intent().setAction(MapsActivity.Filter);
        asyncTaskDatabase = new AsyncTaskDatabase(getApplicationContext(), this);
        asyncTaskDatabase.getUser();
        initializeLocationManager();
        buildNotification();
    }


    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
        SharedPreferences sharedPreferences = getSharedPreferences(MapsActivity.SharedTag, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(MapsActivity.SharedRunnerIsStarted, false);
        editor.commit();
        notificationManager.cancel(1);
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void buildNotification(){
        nofificationBuilder = new NotificationCompat.Builder(this, "CHANNEL_ID");
        nofificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setTicker(String.valueOf(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(String.valueOf(R.string.app_name))
                .setContentText("Your tracker is running")
                .setOngoing(true);

        Intent mapsActivityIntent = new Intent(this, MapsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, mapsActivityIntent, 0);
        nofificationBuilder.setContentIntent(contentIntent);
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, nofificationBuilder.build());
    }

    @Override
    public void insertUserResponse() {}

    @Override
    public void getAllResponse(List<User> users) {}

    @Override
    public void updateResponse() {sendBroadcast(intent);}

    @Override
    public void deleteRsponde() {

    }

}
