package com.esri.arcgisruntime.basicandroidproject;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PopulateDatabase extends Thread {
    List<String> dates = new ArrayList<>();
    List<String> datesPrevList = new ArrayList<>();
    List<Double> highTemps = new ArrayList<>();
    List<Double> lowTemps = new ArrayList<>();
    List<Double> precipitations = new ArrayList<>();
    List<Double> highTempsPrev = new ArrayList<>();
    List<Double> lowTempsPrev = new ArrayList<>();
    List<Double> precipitationsPrev = new ArrayList<>();

    public PopulateDatabase(List<String> dates, List<String> datesPrevList, List<Double> highTemps,
                            List<Double> lowTemps, List<Double> precipitations, List<Double> highTempsPrev,
                            List<Double> lowTempsPrev, List<Double> precipitationsPrev) {

        this.dates = dates;
        this.datesPrevList = datesPrevList;
        this.highTemps = highTemps;
        this.lowTemps = lowTemps;
        this.precipitations = precipitations;
        this.highTempsPrev = highTempsPrev;
        this.lowTempsPrev = lowTempsPrev;
        this.precipitationsPrev = precipitationsPrev;

    }

    public void populateDatabase() {
        WeatherDataRoomDatabase db = WeatherDataRoomDatabase.getInstance(MainActivity.context);
        db.weatherDataDao().deleteAll(); // empty the existing database (if applicable)
            // add the data to the database
            for(int i = 0; i < dates.size() - 1; i++) {
                WeatherData data = new WeatherData(dates.get(i), lowTemps.get(i), highTemps.get(i), precipitations.get(i));
                db.weatherDataDao().insert(data);
            }

        }



}
