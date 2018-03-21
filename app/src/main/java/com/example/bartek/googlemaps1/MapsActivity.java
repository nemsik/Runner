package com.example.bartek.googlemaps1;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;
import com.example.bartek.googlemaps1.DetailsActivities.DetailsActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MapsActivity extends FragmentActivity implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback {

    public static final String TAG = "MapsActivity";
    public static final String Filter = "GpsIntentFilter";
    public static final String SharedTag = "SharedPreferencesRunner";
    public static final String SharedRunnerIsStarted = "runnerisStarted";
    public static final String statusTAG = "LocationStatus";
    private Button bStartStop, bHistory;
    private TextView textViewTime, textViewDistance, textViewSpeed, textViewAvgSpeed;
    private User user;
    private UserDao userDao;
    private List<User> users;
    private ArrayList<Double> userSpeed;
    private double speed = 0, distance = 0, avgSpeed = 0;
    private long userStartTime = 0;
    private Handler handler = new Handler();
    private boolean permissionGranted, runnerisStarted = false;
    private GoogleMap mMap;
    private PolylineOptions rectOptions;
    private LatLng latLng;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private IntentFilter intentFilter = new IntentFilter(Filter);
    private Intent gpsService, historyIntent, detailsIntent;
    private BroadcastReceiver broadcastReceiver;
    private LocationManager manager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        bStartStop = (Button) findViewById(R.id.buttonStartStop);
        bHistory = (Button) findViewById(R.id.buttonHistory);
        textViewDistance = (TextView) findViewById(R.id.textViewDistance);
        textViewSpeed = (TextView) findViewById(R.id.textViewSpeed);
        textViewAvgSpeed = (TextView) findViewById(R.id.textViewAvgSpeed);
        textViewTime = (TextView) findViewById(R.id.textViewTime);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);
        rectOptions = new PolylineOptions();

        bStartStop.setOnClickListener(new bStartStopClick());
        bHistory.setOnClickListener(new bHistoryClick());

        initializeMapsActivity();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    setGUI();
                } catch (Exception e) {
                    Log.i(TAG, "SetGUI err");
                }
            }
        };
    }

    private void initializeMapsActivity() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        userDao = db.userDao();
        gpsService = new Intent(this, GpsService.class);
        historyIntent = new Intent(this, HistoryActivity.class);
        detailsIntent = new Intent(this, DetailsActivity.class);
        sharedPreferences = getSharedPreferences(SharedTag, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                checkPermissions();
            }
        });
    }

    private boolean checkPermissions() {
        permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        if (!mMap.isMyLocationEnabled() && permissionGranted) mMap.setMyLocationEnabled(true);
        return permissionGranted;
    }

    private void startRunner() {
        if (!permissionGranted) return;
        userSpeed = new ArrayList<>();
        runnerisStarted = true;
        bStartStop.setText("Stop");
        user = new User();
        userStartTime = Calendar.getInstance().getTimeInMillis();
        user.setStart_time(userStartTime);
        user.setEnd_time(userStartTime);
        userDao.insert(user);
        if (!isMyServiceRunning(GpsService.class)) startService(gpsService);
        rectOptions = new PolylineOptions();
        registerReceiver(broadcastReceiver, intentFilter);
        handler.postDelayed(runnable, 1000);
    }

    private void stopRunner() {
        bStartStop.setText("Start");
        runnerisStarted = false;
        mMap.clear();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            Log.i(TAG, "can't unregister receiver");
        }
        try {
            stopService(gpsService);
        } catch (Exception e) {
            Log.i(TAG, "can't stop gpsService");
        }
        try {
            handler.removeCallbacks(runnable);
        } catch (Exception e) {
            Log.i(TAG, "can't remove callbacks");
        }
        appLog();
        user = userDao.getUser();
        saveState();
        if (user.getLatitude().size() < 10) buildAlertMessageShortTrack();
        else {
            detailsIntent.putExtra(DetailsActivity.IntentTag, user.getId());
            startActivity(detailsIntent);
        }
    }

    private void continueRunner() {
        runnerisStarted = true;
        bStartStop.setText("Stop");
        user = new User();
        user = userDao.getUser();

        countAvgSpeed();

        rectOptions = new PolylineOptions();
        for (int i = 0; i < user.getLatitude().size() - 1; i++) {
            latLng = new LatLng(user.getLatitude().get(i), user.getLongitude().get(i));
            rectOptions.add(latLng);
        }
        registerReceiver(broadcastReceiver, intentFilter);
        if (!isMyServiceRunning(GpsService.class)) startService(gpsService);

        userStartTime = user.getStart_time();
        handler.postDelayed(runnable, 1000);
    }

    private void countAvgSpeed(){
        double difftime = (user.getEnd_time() - user.getStart_time()) / 1000;
        avgSpeed = (distance / difftime) * 3.6;
        Log.d(TAG, "countAvgSpeed: " + avgSpeed);
    }


    private void drawRoute(double latitude, double longitude) {
        latLng = new LatLng(latitude, longitude);
        rectOptions.add(latLng);
        mMap.addPolyline(rectOptions);
    }

    private void setGUI() {
        user = userDao.getUser();
        drawRoute(user.getLastLatitude(), user.getLastLongitude());
        distance = user.getDistance();
        textViewDistance.setText(String.format("%.2f", distance / 1000));
        speed = user.getLastSpeed();
        textViewSpeed.setText(String.format("%.2f", speed));
        countAvgSpeed();
        textViewAvgSpeed.setText(String.format("%.2f", avgSpeed));
        if (speed < 30) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        else mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
    }


    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long milisecondTime = Calendar.getInstance().getTimeInMillis() - userStartTime;
            int seconds = (int) (milisecondTime / 1000);
            Log.d(TAG, "run: " + milisecondTime);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds %= 60;
            minutes %= 60;
            hours %= 60;
            textViewTime.setText(String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
            handler.postDelayed(this, 1000);
        }
    };


    private void saveState() {
        editor.putBoolean(SharedRunnerIsStarted, runnerisStarted);
        editor.commit();
    }

    private void loadState() {
        runnerisStarted = sharedPreferences.getBoolean(SharedRunnerIsStarted, false);
        Log.d(TAG, "loadState: " + runnerisStarted);
        if (runnerisStarted) continueRunner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadState();
        Log.d(TAG, "onResume: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        if (runnerisStarted) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.d(TAG, "can't unregister receiver");
            }
            try {
                handler.removeCallbacks(runnable);
            } catch (Exception e) {
                Log.d(TAG, "can't remove callbacks");
            }
        }
        saveState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
        if (runnerisStarted) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.d(TAG, "can't unregister receiver");
            }
            try {
                handler.removeCallbacks(runnable);
            } catch (Exception e) {
                Log.d(TAG, "can't remove callbacks");
            }
        }
        saveState();
    }

    private void appLog() {
        users = userDao.getAll();
        for (int i = 0; i < users.size(); i++) {
            StringBuilder stringBuilder = new StringBuilder();
            double speed = 0.0;
            for (int j = 0; j < users.get(i).getSpeed().size(); j++)
                speed += users.get(i).getSpeed().get(j);
            speed /= users.get(i).getSpeed().size();
            stringBuilder.append(users.get(i).getId()).append(" ");
            stringBuilder.append(users.get(i).getStart_time()).append(" ");
            stringBuilder.append(users.get(i).getEnd_time()).append("\n");
            stringBuilder.append(speed).append("\n");
            stringBuilder.append(users.get(i).getLatitude().toString()).append("\n");
            stringBuilder.append(users.get(i).getLongitude().toString()).append("\n\n");
            Log.d(TAG, stringBuilder.toString());
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
    }

    private void buildAlertMessageShortTrack() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("The distance is short, do you want to delete it?");
        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                userDao.delete(user);
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                detailsIntent.putExtra(DetailsActivity.IntentTag, user.getId());
                startActivity(detailsIntent);
            }
        }).setCancelable(false);
        alertDialogBuilder.create().show();
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean checkisGPSenabled() {
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return true;
        return false;
    }

    class bStartStopClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!runnerisStarted) {
                if(!checkisGPSenabled()) buildAlertMessageNoGps();
                else startRunner();
            } else stopRunner();
        }
    }

    class bHistoryClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            startActivity(historyIntent);
        }
    }
}