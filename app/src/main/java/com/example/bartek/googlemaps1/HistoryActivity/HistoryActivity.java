package com.example.bartek.googlemaps1.HistoryActivity;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.bartek.googlemaps1.AsyncTaskDatabase;
import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;
import com.example.bartek.googlemaps1.DetailsActivities.DetailsActivity;
import com.example.bartek.googlemaps1.R;

import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity implements AsyncTaskDatabase.AsyncResponse {

    private static final String TAG = "HistoryActivity";
    private Context context;
    private ListView listView;
    private HistoryAdpater adpater;
    private AsyncTaskDatabase asyncTaskDatabase;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        context = getApplicationContext();
        asyncTaskDatabase = new AsyncTaskDatabase(context, this);
        asyncTaskDatabase.getAll();

    }

    private void setGui() {
        listView = (ListView) findViewById(R.id.listview);
        adpater = new HistoryAdpater(context, R.layout.historyadapter, userList);
        listView.setAdapter(adpater);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent detailsActivity = new Intent(context, DetailsActivity.class);
                detailsActivity.putExtra(DetailsActivity.IntentTag, userList.get(i).getId());
                startActivity(detailsActivity);
            }
        });
    }

    @Override
    public void getAllResponse(List<User> users) {
        userList = users;
        Collections.reverse(userList);
        setGui();
    }

    @Override
    public void insertUserResponse() {}
    @Override
    public void getUserResponse(User user) {}
    @Override
    public void updateResponse() {}
    @Override
    public void deleteRsponse() {}
}

