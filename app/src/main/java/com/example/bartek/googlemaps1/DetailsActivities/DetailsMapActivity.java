package com.example.bartek.googlemaps1.DetailsActivities;

import android.arch.persistence.room.Room;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;
import com.example.bartek.googlemaps1.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class DetailsMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String TAG = "DetailsMapsActivity";
    private User user;
    private UserDao userDao;
    private int userID;
    private GoogleMap mMap;
    private PolylineOptions rectOptions;
    private LatLng latLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_map);

        initializeDetailsActivity();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentInfo);
        mapFragment.getMapAsync(this);
    }

    private void initializeDetailsActivity() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        userDao = db.userDao();
        loadUser();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        });

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMap.getUiSettings().setScrollGesturesEnabled(false);
                mMap.getUiSettings().setZoomGesturesEnabled(false);
                if(user.getLatitude().size() > 2){
                    addMarkers();
                    drawRoute();
                    boundsBulider();
                }

            }
        });
    }

    private void loadUser() {
        user = new User();
        userID = getIntent().getIntExtra(IntentTag, 1);
        user = userDao.getById(userID);
        Log.d(TAG, "loadUser: " + user.getId());
    }

    private void boundsBulider(){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(int i=0; i<user.getLatitude().size(); i++){
            builder.include(new LatLng(user.getLatitude().get(i), user.getLongitude().get(i)));
        }
        LatLngBounds bounds = builder.build();
        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width * 0.12);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.moveCamera(cameraUpdate);
    }

    private void drawRoute() {
        rectOptions = new PolylineOptions();
        for (int i = 0; i < user.getLatitude().size(); i++) {
            latLng = new LatLng(user.getLatitude().get(i), user.getLongitude().get(i));
            rectOptions.add(latLng);
        }
        mMap.addPolyline(rectOptions);
    }

    private void addMarkers(){
        LatLng start = new LatLng(user.getLatitude().get(0), user.getLongitude().get(0));
        LatLng end = new LatLng(user.getLastLatitude(), user.getLastLongitude());
        Marker startMarker = mMap.addMarker(new MarkerOptions().position(start).title("Start"));
        Marker endMarker = mMap.addMarker(new MarkerOptions().position(end).title("End"));
        startMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        startMarker.showInfoWindow();
    }
}
