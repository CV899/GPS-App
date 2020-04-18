package com.esri.arcgisruntime.basicandroidproject;

import androidx.room.*;

@Dao
public interface WeatherDataDao {

    @Query("DELETE FROM weather_data")
    void deleteAll();

    @Insert
    void insert(WeatherData weatherData);

    @Query("SELECT mintemp FROM weather_data WHERE date BETWEEN :minDate AND :maxDate")
    double[] getMinTemps(String minDate, String maxDate);

    @Query("SELECT maxtemp FROM weather_data WHERE date BETWEEN :minDate AND :maxDate")
    double[] getMaxTemps(String minDate, String maxDate);

    @Query("SELECT precipitation FROM weather_data WHERE date BETWEEN :minDate AND :maxDate")
    double[] getPrecipitation(String minDate, String maxDate);

}
