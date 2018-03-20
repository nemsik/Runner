package com.example.bartek.googlemaps1;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;

import java.util.Date;
import java.util.List;

/**
 * Created by bartek on 18.03.2018.
 */

public class HistoryAdpater extends ArrayAdapter<User> {

    private final static String TAG = "ListViewAdapter";

    private Context context;

    public HistoryAdpater(Context context, int resource, List<User> items) {
        super(context, resource, items);
        this.context = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;


        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.listviewadapter, null);
        }

        TextView textViewId = (TextView) v.findViewById(R.id.textViewID);
        TextView textViewTimeStart = (TextView)v.findViewById(R.id.textViewStartTime);

        User u = getItem(position);
        Log.d(TAG, "getView: " + u.toString());

        if (u != null) {
            //todo
            Log.d(TAG, "getView: " + u.getId());
            textViewId.setText(u.getId()+"");
            Date date = new Date();
            date.setTime(u.getStart_time());
            textViewTimeStart.setText(date.toString()+"");

        }

        return v;
    }
}
