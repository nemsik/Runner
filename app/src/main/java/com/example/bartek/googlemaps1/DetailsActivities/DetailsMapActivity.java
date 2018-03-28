package com.example.bartek.googlemaps1.DetailsActivities;

import android.arch.persistence.room.Room;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bartek.googlemaps1.AsyncTaskDatabase;
import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;
import com.example.bartek.googlemaps1.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class DetailsMapActivity extends Fragment implements AsyncTaskDatabase.AsyncResponse {

    public static final String TAG = "DetailsMapsActivity";
    private AsyncTaskDatabase asyncTaskDatabase;
    private User user;
    private int userID;
    private GoogleMap mMap;
    private MapView mMapView;
    private PolylineOptions rectOptions;
    private LatLng latLng;



    public static DetailsMapActivity newInstance(int userID){
        DetailsMapActivity fragmentMaps = new DetailsMapActivity();
        Bundle args = new Bundle();
        args.putInt("userID", userID);
        fragmentMaps.setArguments(args);
        return fragmentMaps;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userID = getArguments().getInt("userID", 1);
        asyncTaskDatabase = new AsyncTaskDatabase(getContext(), this);
        asyncTaskDatabase.getUserById(userID);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_details_map, container,false);

        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();

        return view;
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


    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


    @Override
    public void insertUserResponse() {

    }

    @Override
    public void getUserResponse(User userResponse) {
        user = userResponse;
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
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
        });
    }

    @Override
    public void getAllResponse(List<User> usersResponse) {

    }

    @Override
    public void updateResponse() {

    }

    @Override
    public void deleteResponse() {

    }
}
