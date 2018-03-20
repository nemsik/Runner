package com.example.bartek.googlemaps1;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bartek.googlemaps1.Database.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by bartek on 18.03.2018.
 */

public class HistoryAdpater extends ArrayAdapter<User> {

    private final static String TAG = "ListViewAdapter";

    private TextView textViewDistance, textViewTime, textViewStartTime;
    private ImageView imageView;
    private long time, starttime, endtime;

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
            v = vi.inflate(R.layout.historyadapter, null);
        }

        textViewDistance = (TextView) v.findViewById(R.id.historyTextViewDistance);
        textViewStartTime = (TextView) v.findViewById(R.id.historyTextViewStartTime);
        textViewTime = (TextView) v.findViewById(R.id.historyTextViewTime);
        imageView = (ImageView) v.findViewById(R.id.historyImageView);

        User user = getItem(position);

        if (user != null) {
            starttime = user.getStart_time();
            endtime = user.getEnd_time();
            Calendar calendar;
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(starttime);

            SimpleDateFormat format = new SimpleDateFormat("EEEE, d MMMM, yyyy");
            textViewStartTime.setText(format.format(calendar.getTime()));
            textViewDistance.setText(String.format("%.2f", user.getDistance()/1000) + " km");

            int seconds = (int) ((endtime - starttime) / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds %= 60;
            minutes %= 60;
            hours %= 60;
            textViewTime.setText(String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
            imageView.setImageResource(R.mipmap.ic_launcher);
        }

        return v;
    }
}
