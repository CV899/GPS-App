package com.esri.arcgisruntime.basicandroidproject;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "US_cities")
public class UScities {

    //TODO: need a primary key and all columns non null?
    @ColumnInfo(name = "mindate")
    private String mindate;
    @ColumnInfo(name = "maxdate")
    private String maxdate;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "datacoverage")
    private String datacoverage;
    @ColumnInfo(name = "id")
    private String id;

    public UScities(String mindate, String maxdate, String name, String datacoverage, String id) {
        this.mindate = mindate;
        this.maxdate = maxdate;
        this.name = name;
        this.datacoverage = datacoverage;
        this.id = id;
    }

    public String getMindate() {
        return this.mindate;
    }

    public String getMaxdate() {
        return this.maxdate;
    }

    public String getName() {
        return this.name;
    }

    public String getDatacoverage() {
        return this.datacoverage;
    }

    public String getId() {
        return this.id;
    }
}
