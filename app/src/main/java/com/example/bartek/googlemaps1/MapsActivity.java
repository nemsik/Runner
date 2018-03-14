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
    private TextView textViewTime, textViewDistance, textViewKcal, textViewRate;

    private User user;
    private UserDao userDao;
    private List<User> users;

    private long userStartTime;
    private ArrayList<Double> userSpeedsList = new ArrayList<>();
    private double userSpeed = 0;
    private double userRate = 0;
    private double userKcal = 0;

    private double userKg = 70;


    private Handler handler = new Handler();

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
        textViewDistance = (TextView) findViewById(R.id.textViewDistance);
        textViewKcal = (TextView) findViewById(R.id.textViewKcal);
        textViewRate = (TextView) findViewById(R.id.textViewRate);
        textViewTime = (TextView) findViewById(R.id.textViewTime);

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
                double distance = user.getDistance();
                distance/=1000;
                textViewDistance.setText(String.format("%.2f",distance));
                userSpeedsList = user.getSpeed();

                userSpeed = userSpeedsList.get(userSpeedsList.size()-1);
                userRate = 60/userSpeed;
                textViewRate.setText(String.format("%.2f", userRate));

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
                    mMap.clear();
                    handler.removeCallbacks(runnable);
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        checkPermissions();
        mMap = googleMap;

        LatLng uniLodz = new LatLng(51.7770423,19.48356);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(uniLodz));
        if (checkPermissions()) mMap.setMyLocationEnabled(true);

    }

    private boolean checkPermissions() {
        permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        return permissionGranted;
    }

    private void startRunner() {
        if (!permissionGranted) return;
        runnerisStarted = true;
        bStartStop.setText("Stop");
        user = new User();
        user.setStart_time(SystemClock.uptimeMillis());
        userDao.insert(user);
        startService(gpsService);
        registerReceiver(broadcastReceiver, intentFilter);

        userStartTime = user.getStart_time();
        handler.postDelayed(runnable, 1000);

    }

    private void continueRunner() {
        if (!permissionGranted) return;
        runnerisStarted = true;
        bStartStop.setText("Stop");
        user = new User();
        user = userDao.getUser();
        rectOptions = new PolylineOptions();
        for(int i=0; i<user.getLatitude().size()-1; i++){
            latLng = new LatLng(user.getLatitude().get(i), user.getLongitude().get(i));
            rectOptions.add(latLng);
        }
        //mMap.addPolyline(rectOptions);
        startService(gpsService);
        registerReceiver(broadcastReceiver, intentFilter);

        userStartTime = user.getStart_time();
        handler.postDelayed(runnable, 1000);

    }


    private void drawRoute(ArrayList<Double> latitude, ArrayList<Double> longitude) {
        int size = latitude.size();
        latLng = new LatLng(latitude.get(size - 1), longitude.get(size - 1));
        rectOptions.add(latLng);

        mMap.addPolyline(rectOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    private void setUi(double distance){
        textViewDistance.setText(String.format("%.2f",distance));
    }


    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long milisecondTime = SystemClock.uptimeMillis() - userStartTime;
            int seconds = (int) (milisecondTime/1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds%=60;
            minutes%=60;
            hours%=60;
            textViewTime.setText(String.format("%02d",hours)+":"+String.format("%02d",minutes)+":"+String.format("%02d",seconds));
            handler.postDelayed(this, 1000);
        }
    };

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
        if (runnerisStarted){
            unregisterReceiver(broadcastReceiver);
            handler.removeCallbacks(runnable);
        }
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
