package com.example.bartek.googlemaps1;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Adapter;
import android.widget.ListView;

import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private Context context;
    private ListView listView;
    private ListViewAdpater adpater;

    private User user;
    private UserDao userDao;

    AppDatabase db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        context = getApplicationContext();

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").fallbackToDestructiveMigration().allowMainThreadQueries().build();

        userDao = db.userDao();

        List<User> userList = userDao.getAll();
        Log.d(TAG, "onCreate: " + userList.toString());


        listView = (ListView)findViewById(R.id.listview);
        adpater = new ListViewAdpater(context, R.layout.listviewadapter ,userList);
        listView.setAdapter(adpater);

    }
}