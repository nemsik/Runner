package com.example.bartek.googlemaps1.Database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bartek on 13.03.2018.
 */
@Entity(tableName = "user")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "start_time")
    private long start_time;

    @ColumnInfo(name = "end_time")
    private long end_time;

    @ColumnInfo(name = "latitude")
    private ArrayList<Double> latitude = new ArrayList<>();

    @ColumnInfo(name = "longitude")
    private ArrayList<Double> longitude = new ArrayList<>();

    @ColumnInfo(name = "spped")
    private ArrayList<Double> speed = new ArrayList<>();

    @ColumnInfo(name = "distance")
    private double distance = 0;

    @ColumnInfo(name = "kcal")
    private double kcal = 0;

    public void addDistance(double distance){
        this.distance+=distance;
    }

    public void addKcal(double kcal){
        this.kcal+=kcal;
    }

    public double getKcal() {
        return kcal;
    }

    public void setKcal(double kcal) {
        this.kcal = kcal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addLatitude(double latitude) {
        this.latitude.add(latitude);
    }

    public ArrayList<Double> getLatitude() {
        return latitude;
    }

    public void addLongitude(double longitude) {
        this.longitude.add(longitude);
    }

    public ArrayList<Double> getLongitude() {
        return longitude;
    }

    public void addSpeed(double speed){
        this.speed.add(speed);
    }

    public ArrayList<Double> getSpeed() {
        return speed;
    }

    public long getStart_time() {
        return start_time;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }

    public void setLatitude(ArrayList<Double> latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(ArrayList<Double> longitude) {
        this.longitude = longitude;
    }

    public void setSpeed(ArrayList<Double> speed) {
        this.speed = speed;
    }

    public double getLastLatitude(){
        return latitude.get(latitude.size()-1);
    }

    public double getLastLongitude(){
        return latitude.get(longitude.size()-1);
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
