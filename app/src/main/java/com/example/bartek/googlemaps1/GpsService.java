package com.example.bartek.googlemaps1;

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
import android.util.Log;

import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Created by bartek on 14.03.2018.
 */

public class GpsService extends Service {
    private static final String TAG = "GpsService";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    private UserDao userDao;
    private User user;
    private Intent intent;
    private double speed;

    private int status = 0;

    private boolean isGpsEnabled = false, isNetworkEnabled = false;


    private Location mLastLocation;

    private class LocationListener implements android.location.LocationListener {

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
            Log.e(TAG, "LocationListener: " + mLastLocation);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            if (mLastLocation.getLongitude() != 0 && mLastLocation.getLongitude() != 0)
                user.addDistance(mLastLocation.distanceTo(location));
            mLastLocation.set(location);
            if (location != null) {
                user.addLatitude(location.getLatitude());
                user.addLongitude(location.getLongitude());
                speed = location.getSpeed();
                speed *= 3.6;
                user.addSpeed(speed);
                user.setEnd_time(Calendar.getInstance().getTimeInMillis());
                userDao.update(user);
                sendBroadcast(intent);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
            checkStatus();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(MapsActivity.statusTAG, status);
            sendBroadcast(intent);
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

        initializeLocationManager();
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

        checkStatus();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(MapsActivity.statusTAG, status);
        sendBroadcast(intent);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        userDao = db.userDao();
        user = userDao.getUser();
    }

    private int checkStatus() {
        isGpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.i(TAG, "GPS ENABLED: " + isGpsEnabled);
        Log.i(TAG, "NETWORK ENABLED " + isNetworkEnabled);

        if (!isGpsEnabled && !isNetworkEnabled) {
            status = 0;
        } else if (!isGpsEnabled & isNetworkEnabled) {
            status = 1;
        } else if (isGpsEnabled) {
            status = 2;
        }
        return status;
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

    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
}
