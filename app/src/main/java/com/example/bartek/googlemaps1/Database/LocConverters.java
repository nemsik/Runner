package com.example.bartek.googlemaps1.Database;

import android.arch.persistence.room.TypeConverter;
import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * Created by bartek on 28.03.2018.
 */

public class LocConverters {

    @TypeConverter
    public static List<Location> stringToSomeObjectList(String value) {
        if (value == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<Location>>() {}.getType();

        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<Location> someObjects) {
        return new Gson().toJson(someObjects);
    }
}
