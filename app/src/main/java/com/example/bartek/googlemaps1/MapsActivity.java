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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

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
import java.util.List;

public class MapsActivity extends FragmentActivity implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback {

    public static final String TAG = "MapsActivity";
    public static final String Filter = "GpsIntentFilter";
    public static final String SharedTag = "SharedPreferencesRunner";
    public static final String SharedRunnerIsStarted = "runnerisStarted";

    private Button bStartStop;

    private User user;
    private UserDao userDao;
    private List<User> users;

    private boolean permissionGranted, runnerisStarted = false;
    private GoogleMap mMap;
    private PolylineOptions rectOptions;
    private LatLng latLng;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    BroadcastReceiver broadcastReceiver;
    IntentFilter intentFilter;
    Intent gpsService;


    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bStartStop = (Button) findViewById(R.id.buttonStartStop);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);
        checkPermissions();

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").fallbackToDestructiveMigration().allowMainThreadQueries().build();

        userDao = db.userDao();

        intentFilter = new IntentFilter(Filter);
        gpsService = new Intent(this, GpsService.class);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: ");
                user = userDao.getUser();
                drawRoute(user.getLatitude(), user.getLongitude());
            }
        };

        rectOptions = new PolylineOptions();

        bStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!runnerisStarted)
                    startRunner();
                else {
                    bStartStop.setText("Start");
                    unregisterReceiver(broadcastReceiver);
                    stopService(gpsService);
                    runnerisStarted = false;
                    appLog();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(37.35, -122.0);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        if (checkPermissions()) mMap.setMyLocationEnabled(true);

    }

    private boolean checkPermissions() {
        permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!permissionGranted)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
        return permissionGranted;
    }

    private void startRunner() {
        if (!permissionGranted) return;
        runnerisStarted = true;
        bStartStop.setText("Stop");
        user = new User();
        user.setStart_time(Calendar.getInstance().getTimeInMillis());
        userDao.insert(user);
        startService(gpsService);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void continueRunner() {
        if (!permissionGranted) return;
        runnerisStarted = true;
        bStartStop.setText("Stop");
        user = new User();
        user = userDao.getUser();
        startService(gpsService);
        registerReceiver(broadcastReceiver, intentFilter);
    }


    private void drawRoute(ArrayList<Double> latitude, ArrayList<Double> longitude) {
        int size = latitude.size();
        latLng = new LatLng(latitude.get(size - 1), longitude.get(size - 1));
        rectOptions.add(latLng);

        mMap.addPolyline(rectOptions);
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

    private void saveState() {
        sharedPreferences = getSharedPreferences(SharedTag, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putBoolean(SharedRunnerIsStarted, runnerisStarted);
        editor.commit();
    }

    private void loadState() {
        sharedPreferences = getSharedPreferences(SharedTag, Context.MODE_PRIVATE);
        runnerisStarted = sharedPreferences.getBoolean(SharedRunnerIsStarted, false);
        Log.d(TAG, "loadState: " + runnerisStarted);
        if (runnerisStarted) continueRunner();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Log.d(TAG, "onMyLocationButtonClick: ");
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Log.d(TAG, "onMyLocationButtonClick:  elo");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (runnerisStarted) unregisterReceiver(broadcastReceiver);
        saveState();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadState();
        Log.d(TAG, "onResume: ");
    }
}
