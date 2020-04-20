package com.esri.arcgisruntime.basicandroidproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.*;

@Entity(tableName = "weather_data")
public class WeatherData {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "date")
    private String date;
    @ColumnInfo(name = "mintemp")
    @Nullable
    private double minTemp;
    @ColumnInfo(name = "maxtemp")
    @Nullable
    private double maxTemp;
    @ColumnInfo(name = "precipitation")
    @Nullable
    private double precipitation;

    public WeatherData(@NonNull String date, double minTemp, double maxTemp, double precipitation) {
        this.date = date;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.precipitation = precipitation;
    }

    public String getDate() {
        return this.date;
    }

    public double getMinTemp() {
        return this.minTemp;
    }

    public double getMaxTemp() {
        return this.maxTemp;
    }

    public double getPrecipitation() {
        return this.precipitation;
    }
}
