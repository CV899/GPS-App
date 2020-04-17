package com.esri.arcgisruntime.basicandroidproject;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.esri.arcgisruntime.basicandroidproject.UScities;

import java.util.List;

@Dao
public interface UScitiesDao {

    @Insert
    void insert(UScities cities);

    @Query("select name from US_cities where name like %:state%")
    public List<String> findCities(String state);

    @Query("select id from US_cities where name = :name")
    public String findID(String name);

}