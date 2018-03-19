package com.example.bartek.googlemaps1;

import android.Manifest;
import android.app.ActivityManager;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private TextView textViewTime, textViewDistance, textViewKcal, textViewRate;
    private User user;
    private UserDao userDao;
    private List<User> users;
    private long userStartTime = 0;
    private double userSpeed = 0, userRate = 0;
    private Handler handler = new Handler();;
    private boolean permissionGranted, runnerisStarted = false;
    private GoogleMap mMap;
    private PolylineOptions rectOptions;
    private LatLng latLng;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private IntentFilter intentFilter = new IntentFilter(Filter);
    private Intent gpsService, historyIntent;
    private BroadcastReceiver broadcastReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bStartStop = (Button) findViewById(R.id.buttonStartStop);
        bHistory = (Button) findViewById(R.id.buttonHistory);
        textViewDistance = (TextView) findViewById(R.id.textViewDistance);
        textViewKcal = (TextView) findViewById(R.id.textViewKcal);
        textViewRate = (TextView) findViewById(R.id.textViewRate);
        textViewTime = (TextView) findViewById(R.id.textViewTime);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);
        rectOptions = new PolylineOptions();

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        userDao = db.userDao();

        gpsService = new Intent(this, GpsService.class);
        historyIntent = new Intent(this, HistoryActivity.class);

        bStartStop.setOnClickListener(new bStartStopClick());
        bHistory.setOnClickListener(new bHistoryClick());

        sharedPreferences = getSharedPreferences(SharedTag, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: " + intent.getIntExtra(statusTAG, 10));
                if (intent.getIntExtra(statusTAG, 0) == 0) {
                    Log.d(TAG, "onReceive:  LOCATION IS DISABLED");
                    stopRunner();
                }
                try {
                    setGUI();
                } catch (Exception e) {
                    Log.i(TAG, "SetGUI err");
                }
            }
        };

        //loadState();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng uniLodz = new LatLng(51.7770423, 19.48356);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(uniLodz));
        checkPermissions();
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
        runnerisStarted = true;
        bStartStop.setText("Stop");
        user = new User();
        userStartTime = Calendar.getInstance().getTimeInMillis();
        user.setStart_time(userStartTime);
        userDao.insert(user);
        registerReceiver(broadcastReceiver, intentFilter);
        if (!isMyServiceRunning(GpsService.class)) startService(gpsService);
        handler.postDelayed(runnable, 1000);
    }

    private void stopRunner() {
        bStartStop.setText("Start");
        try {
            unregisterReceiver(broadcastReceiver);
            stopService(gpsService);
        }catch (Exception e){
            Log.i(TAG, "receiver is not registered");
        }
        runnerisStarted = false;
        mMap.clear();
        handler.removeCallbacks(runnable);
        appLog();
    }

    private void continueRunner() {
        runnerisStarted = true;
        bStartStop.setText("Stop");
        user = new User();
        user = userDao.getUser();
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


    private void drawRoute(double latitude, double longitude) {
        latLng = new LatLng(latitude, longitude);
        rectOptions.add(latLng);
        mMap.addPolyline(rectOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    private void setGUI() {
        user = userDao.getUser();
        drawRoute(user.getLastLatitude(), user.getLastLongitude());
        double distance = user.getDistance() / 1000;
        textViewDistance.setText(String.format("%.2f", distance));
        userSpeed = user.getLastSpeed();
        userRate = 60 / userSpeed;
        textViewRate.setText(String.format("%.2f", userRate));
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
            }catch (Exception e){}
            try {
                handler.removeCallbacks(runnable);
            }catch (Exception e){}
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
            }catch (Exception e){}
            try {
                handler.removeCallbacks(runnable);
            }catch (Exception e){}
        }
        saveState();
    }

    private void appLog() {
        users = userDao.getAll();
        Log.d(TAG, "" + users.size());
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

    class bStartStopClick implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            if (!runnerisStarted)
                startRunner();
            else stopRunner();
        }
    }

    class bHistoryClick implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            startActivity(historyIntent);
        }
    }
}
