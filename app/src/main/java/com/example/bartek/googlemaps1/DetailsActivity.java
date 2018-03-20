package com.example.bartek.googlemaps1;

import android.arch.persistence.room.Room;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;

public class DetailsActivity extends AppCompatActivity {
    private static final String TAG = "DetailsActivity";
    public static final String IntentTag = "userid";

    private User user;
    private UserDao userDao;
    private int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        initializeDetailsActivity();
    }

    private void initializeDetailsActivity() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        userDao = db.userDao();
        loadUser();
    }

    private void loadUser() {
        user = new User();
        userID = getIntent().getIntExtra(IntentTag, 1);
        user = userDao.getById(userID);
        Log.d(TAG, "loadUser: " + user.getId());
    }
}
