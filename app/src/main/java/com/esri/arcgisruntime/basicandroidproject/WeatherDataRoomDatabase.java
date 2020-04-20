package com.esri.arcgisruntime.basicandroidproject;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(entities = {WeatherData.class}, version = 1, exportSchema = false)
public abstract class WeatherDataRoomDatabase extends RoomDatabase {

    public abstract WeatherDataDao weatherDataDao();
    private static WeatherDataRoomDatabase INSTANCE;

    public static synchronized WeatherDataRoomDatabase getInstance(Context context) {
        if(INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), WeatherDataRoomDatabase.class,
                    "weather_data.db").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        }

        return INSTANCE;
    }

}
