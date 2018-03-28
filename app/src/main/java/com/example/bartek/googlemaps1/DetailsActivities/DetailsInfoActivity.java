package com.example.bartek.googlemaps1.DetailsActivities;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.bartek.googlemaps1.AsyncTaskDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DetailsInfoActivity extends Fragment implements AsyncTaskDatabase.AsyncResponse {

    public final static String TAG = "DetailsInfoActivity";
    private int userID;

    private TextView TextViewStartTime, TextViewEndTime, TextViewTime, TextViewMaxSpeed, TextViewAvgSpeed, TextViewDistance;
    private AsyncTaskDatabase asyncTaskDatabase;
    private ArrayList<Double> userSpeeds;
    private LineChart lineChart;
    double difftime;
    private String text;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userID = getArguments().getInt("userID", 1);
    }

    public static DetailsInfoActivity newInstance(int userID){
        DetailsInfoActivity fragmetInfo = new DetailsInfoActivity();
        Bundle args = new Bundle();
        args.putInt("userID", userID);
        fragmetInfo.setArguments(args);
        return fragmetInfo;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_details_info, container, false);

        TextViewStartTime = (TextView)view.findViewById(R.id.textViewInfoStartTime);
        TextViewEndTime = (TextView)view.findViewById(R.id.textViewInfoEndTime);
        TextViewTime = (TextView)view.findViewById(R.id.textViewInfoTime);
        TextViewMaxSpeed = (TextView)view.findViewById(R.id.textViewInfoMaxSpeed);
        TextViewAvgSpeed = (TextView)view.findViewById(R.id.textViewInfoAvgSpeed);
        TextViewDistance = (TextView)view.findViewById(R.id.textViewInfoDistance);

        lineChart = (LineChart) view.findViewById(R.id.barChart) ;

        asyncTaskDatabase = new AsyncTaskDatabase(getContext(), this);
        asyncTaskDatabase.getUserById(userID);

        return view;
    }

    @Override
    public void getUserResponse(User userResponse) {
        Log.d(TAG, "getUserResponse: " + userResponse.toString());

        userSpeeds = userResponse.getSpeed();
        Log.d(TAG, "size userspeed" + userSpeeds.size());

        Calendar calendar;
        calendar = Calendar.getInstance();

        difftime = (userResponse.getEnd_time() - userResponse.getStart_time()) / 1000;
        double distance = userResponse.getDistance();

        calendar.setTimeInMillis(userResponse.getStart_time());
        SimpleDateFormat format = new SimpleDateFormat("hh:mm, EEEE, d MMMM, yyyy");
        TextViewStartTime.setText(format.format(calendar.getTime()));

        calendar.setTimeInMillis(userResponse.getEnd_time());
        TextViewEndTime.setText(format.format(calendar.getTime()));

        int seconds = (int) ((difftime));
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        hours %= 60;
        text = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        TextViewTime.setText(text);
        double maxSpeed = 0;
        for(int i=0; i<userSpeeds.size(); i++) if(userSpeeds.get(i) > maxSpeed) maxSpeed = userSpeeds.get(i);
        TextViewMaxSpeed.setText(String.format("%.2f", maxSpeed) + " km/h");
        TextViewAvgSpeed.setText(String.format("%.2f", (distance/difftime) * 3.6) + " km/h");
        TextViewDistance.setText(String.format("%.2f", userResponse.getDistance()/1000) + " km");

        drawChart();
    }

    private void drawChart(){
        ArrayList<Entry> entries = new ArrayList<>();
        for(int i=0; i<userSpeeds.size(); i++){
            double dspeed = userSpeeds.get(i);
            float fspeed = (float) dspeed;
            entries.add(new BarEntry(i, fspeed));
        }


        LineDataSet dataset = new LineDataSet(entries, "# of Calls");
        dataset.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataset.setCubicIntensity(0.1f);
        dataset.setDrawFilled(true);
        dataset.setDrawCircles(false);
        dataset.setLineWidth(4.8f);
        dataset.setColor(Color.RED);
        dataset.setFillColor(Color.RED);
        dataset.setFillAlpha(100);
        dataset.setDrawHorizontalHighlightIndicator(true);
        dataset.setDrawValues(false);
        lineChart.getAxisLeft().setDrawLabels(true);
        lineChart.getAxisRight().setDrawLabels(false);
        lineChart.getXAxis().setDrawLabels(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setAxisMinimum(0);
        xAxis.setAxisMaximum(userSpeeds.size()-1); // because there are 250 data points
        xAxis.setLabelCount(userSpeeds.size()-1);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        lineChart.getLegend().setEnabled(false);

        LineData data = new LineData(dataset);
        lineChart.setData(data);
        lineChart.notifyDataSetChanged();
    }


    @Override
    public void insertUserResponse() {}

    @Override
    public void getAllResponse(List<User> usersResponse) {}

    @Override
    public void updateResponse() {}

    @Override
    public void deleteResponse() {}
}
