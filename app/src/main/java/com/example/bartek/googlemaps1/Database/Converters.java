package com.example.bartek.googlemaps1.Database;

import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by bartek on 13.03.2018.
 */

public class Converters {
    @TypeConverter
    public static ArrayList<Double> fromString(String value) {
        Type listType = new TypeToken<ArrayList<Double>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromArrayList(ArrayList<Double> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }

}
