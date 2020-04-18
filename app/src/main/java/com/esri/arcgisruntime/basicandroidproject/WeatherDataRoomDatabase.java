package com.esri.arcgisruntime.basicandroidproject;

import android.content.Context;

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

    private static volatile WeatherDataRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static WeatherDataRoomDatabase getDatabase(final Context context) {
        if(INSTANCE == null) {
            synchronized (WeatherDataRoomDatabase.class) {
                if(INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            WeatherDataRoomDatabase.class, "weather_data").build();
                }
            }
        }

        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            // If you want to keep data through app restarts,
            // comment out the following block
            databaseWriteExecutor.execute(() -> {
                // Populate the database in the background.
                // If you want to start with more words, just add them.
                WeatherDataDao dao = INSTANCE.weatherDataDao();
                dao.deleteAll();

            });
        }
    };
}
