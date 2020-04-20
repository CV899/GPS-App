package com.esri.arcgisruntime.basicandroidproject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.room.Room;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Results extends Activity {

    String location;
    TextView displayResults;
    static String API_KEY = "CUddzkJaeBvQVYmVAgjLnkPALBpgeogF";
    JSONArray maxTempResults = null;
    JSONArray minTempResults;
    JSONArray precipitationResults;
    String responseString;
    LocalDate dateSelected;
    String dateSelectedString;
    LocalDate startDate;
    LocalDate endDate;
    LocalDate date;
    LocalDate startDatePrev;
    LocalDate endDatePrev;
    int i;
    double sum = 0;
    double average = 0;
    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-M-dd");
    List<Double> tempValues = new ArrayList<>();
    List<String> datesPrevList = new ArrayList<>();
    List<Double> lowTemps = new ArrayList<>();
    List<Double> precipitations = new ArrayList<>();
    List<Double> highTempsPrev = new ArrayList<>();
    List<Double> lowTempsPrev = new ArrayList<>();
    List<String> dates = new ArrayList<>();
    List<Double> highTemps = new ArrayList<>();
    List<Double> precipitationsPrev = new ArrayList<>();
    RequestQueue queue;


    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.results);
        Intent in = getIntent();
        displayResults = (TextView) findViewById(R.id.displayResults);
        location = in.getStringExtra("location");
        dateSelectedString = in.getStringExtra("date");
        dateSelected = LocalDate.parse(dateSelectedString, format);
        startDate = dateSelected.minusDays(7);
        endDate = dateSelected.minusDays(1);
        startDatePrev = dateSelected.minusYears(1).minusDays(7);
        endDatePrev = dateSelected.minusYears(1).plusDays(7);
        queue = Volley.newRequestQueue(this);

        onPreExecute();

        // initiates a chain of volley calls that pass the API data down the line;
        // ends in populating the database and using the sliding window algorithm to predict weather
        try {
            firstVolleyCall();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onPreExecute() {
        displayResults.setText("Calculating...");
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void firstVolleyCall() throws JSONException {
        String url = "https://www.ncdc.noaa.gov/cdo-web/api/v2/data?datasetid=GHCND&locationid="
                + location + "&startdate=" + startDate.toString() + "&enddate=" + endDate.toString() + "&datatypeid=TMAX&units=standard&limit=1000";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    responseString = response.toString();
                    JSONObject obj = new JSONObject(responseString);
                    maxTempResults = obj.getJSONArray("results");
                    // calculate high temp
                    date = startDate;
                    JSONObject data = maxTempResults.getJSONObject(0);
                    i = 0;
                    while(!date.toString().equals(endDate.plusDays(1).toString())) {
                        int size = maxTempResults.length();
                        StringBuilder dataDate = new StringBuilder(data.getString("date"));
                        dataDate.delete(dataDate.length() - 9, dataDate.length());
                        while(dataDate.toString().equals(date.toString())) {

                            tempValues.add(Double.parseDouble(data.get("value").toString()));
                            i++;
                            if(i > maxTempResults.length() - 1) {
                                break;
                            } else {
                                data = maxTempResults.getJSONObject(i);
                                dataDate = new StringBuilder(data.getString("date"));
                                dataDate.delete(dataDate.length() - 9, dataDate.length());
                            }
                        }

                        for(int i = 0; i < tempValues.size(); i++) {
                            sum = (double) (sum + tempValues.get(i));
                        }
                        average = ((double) sum / tempValues.size());
                        highTemps.add(average);
                        dates.add(date.toString());
                        date = date.plusDays(1);
                        sum = 0;
                        tempValues = new ArrayList<>();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    secondVolleyCall(dates, highTemps);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                error.printStackTrace();

            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("token", API_KEY);
                return params;
            }
        };
        queue.add(jsonObjectRequest);

    }

    protected void secondVolleyCall(List<String> newDates, List<Double> newHighTemps) throws JSONException {
        String precipitatonUrl = "https://www.ncdc.noaa.gov/cdo-web/api/v2/data?datasetid=GHCND&locationid="
                + location + "&startdate=" + startDate.toString() + "&enddate=" + endDate.toString() + "&datatypeid=PRCP&units=standard&limit=1000";
        JsonObjectRequest getPrecipitation = new JsonObjectRequest(Request.Method.GET, precipitatonUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dates = newDates;
                highTemps = newHighTemps;
                try {
                    responseString = response.toString();
                    JSONObject obj = new JSONObject(responseString);
                    precipitationResults = obj.getJSONArray("results");
                    date = startDate;
                    JSONObject data = precipitationResults.getJSONObject(0);
                    i = 0;
                    while (!date.toString().equals(endDate.plusDays(1).toString())) {

                        StringBuilder dataDate = new StringBuilder(data.getString("date"));
                        dataDate.delete(dataDate.length() - 9, dataDate.length());
                        while (dataDate.toString().equals(date.toString())) {

                            tempValues.add(Double.parseDouble(data.get("value").toString()));
                            i++;
                            if (i > precipitationResults.length() - 1) {
                                break;
                            } else {
                                data = precipitationResults.getJSONObject(i);
                                dataDate = new StringBuilder(data.getString("date"));
                                dataDate.delete(dataDate.length() - 9, dataDate.length());
                            }
                        }

                        for (int i = 0; i < tempValues.size(); i++) {
                            sum = (double) (sum + tempValues.get(i));
                        }
                        average = ((double) sum / tempValues.size());
                        precipitations.add(average);
                        date = date.plusDays(1);
                        sum = 0;
                        tempValues = new ArrayList<>();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    thirdVolleyCall(precipitations);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("token", API_KEY);
                return params;
            }
        };

        queue.add(getPrecipitation);

    }

    protected void thirdVolleyCall(List<Double> newPrecipitations) throws JSONException {

        precipitations = newPrecipitations;
        String minTempsUrl = "https://www.ncdc.noaa.gov/cdo-web/api/v2/data?datasetid=GHCND&locationid="
                + location + "&startdate=" + startDate.toString() + "&enddate=" + endDate.toString() + "&datatypeid=TMIN&units=standard&limit=1000";
        JsonObjectRequest getMinTemps = new JsonObjectRequest(Request.Method.GET, minTempsUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    responseString = response.toString();
                    JSONObject obj = new JSONObject(responseString);
                    minTempResults = obj.getJSONArray("results");
                    date = startDate;
                    JSONObject data = minTempResults.getJSONObject(0);
                    i = 0;
                    while (!date.toString().equals(endDate.plusDays(1).toString())) {

                        StringBuilder dataDate = new StringBuilder(data.getString("date"));
                        dataDate.delete(dataDate.length() - 9, dataDate.length());
                        while (dataDate.toString().equals(date.toString())) {

                            tempValues.add(Double.parseDouble(data.get("value").toString()));
                            i++;
                            if (i > minTempResults.length() - 1) {
                                break;
                            } else {
                                data = minTempResults.getJSONObject(i);
                                dataDate = new StringBuilder(data.getString("date"));
                                dataDate.delete(dataDate.length() - 9, dataDate.length());
                            }
                        }

                        for (int i = 0; i < tempValues.size(); i++) {
                            sum = (double) (sum + tempValues.get(i));
                        }
                        average = ((double) sum / tempValues.size());
                        lowTemps.add(average);
                        date = date.plusDays(1);
                        sum = 0;
                        tempValues = new ArrayList<>();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    fourthVolleyCall(lowTemps);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("token", API_KEY);
                return params;
            }
        };
        queue.add(getMinTemps);

    }

    protected void fourthVolleyCall(List<Double> newLowTemps) throws JSONException {
        lowTemps = newLowTemps;

        String maxTempsUrlPrev = "https://www.ncdc.noaa.gov/cdo-web/api/v2/data?datasetid=GHCND&locationid="
                + location + "&startdate=" + startDatePrev.toString() + "&enddate=" + endDatePrev.toString() + "&datatypeid=TMAX&units=standard&limit=1000";
        JsonObjectRequest getHighTempsPrev = new JsonObjectRequest(Request.Method.GET, maxTempsUrlPrev, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    responseString = response.toString();
                    JSONObject obj = new JSONObject(responseString);
                    maxTempResults = obj.getJSONArray("results");
                    // calculate high temp
                    date = startDatePrev;
                    JSONObject data = maxTempResults.getJSONObject(0);
                    i = 0;
                    while(!date.toString().equals(endDatePrev.plusDays(1).toString())) {
                        int size = maxTempResults.length();
                        StringBuilder dataDate = new StringBuilder(data.getString("date"));
                        dataDate.delete(dataDate.length() - 9, dataDate.length());
                        while(dataDate.toString().equals(date.toString())) {
                            tempValues.add(Double.parseDouble(data.get("value").toString()));
                            i++;
                            if(i > maxTempResults.length() - 1) {
                                break;
                            } else {
                                data = maxTempResults.getJSONObject(i);
                                dataDate = new StringBuilder(data.getString("date"));
                                dataDate.delete(dataDate.length() - 9, dataDate.length());
                            }
                        }

                        for(int i = 0; i < tempValues.size(); i++) {
                            sum = (double) (sum + tempValues.get(i));
                        }
                        average = ((double) sum / tempValues.size());
                        highTempsPrev.add(average);
                        datesPrevList.add(date.toString());
                        date = date.plusDays(1);
                        sum = 0;
                        tempValues = new ArrayList<>();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    fifthVolleyCall(datesPrevList, highTempsPrev);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("onErrorInvoked", "true");
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("token", API_KEY);
                return params;
            }
        };

        queue.add(getHighTempsPrev);
    }

    protected void fifthVolleyCall(List<String> newDatesPrevList, List<Double> newHighTempsPrev) throws JSONException {
        datesPrevList = newDatesPrevList;
        highTempsPrev = newHighTempsPrev;
        String precipitatonUrlPrev = "https://www.ncdc.noaa.gov/cdo-web/api/v2/data?datasetid=GHCND&locationid="
                + location + "&startdate=" + startDatePrev.toString() + "T00:00:00&enddate=" + endDatePrev.toString() + "T00:00:00&datatypeid=PRCP&units=standard&limit=1000";
        JsonObjectRequest getPrecipitationPrev = new JsonObjectRequest(Request.Method.GET, precipitatonUrlPrev, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d("index5", "" + dates.size());
                    responseString = response.toString();
                    JSONObject obj = new JSONObject(responseString);
                    precipitationResults = obj.getJSONArray("results");
                    date = startDatePrev;
                    JSONObject data = precipitationResults.getJSONObject(0);
                    i = 0;
                    while (!date.toString().equals(endDatePrev.plusDays(1).toString())) {

                        StringBuilder dataDate = new StringBuilder(data.getString("date"));
                        dataDate.delete(dataDate.length() - 9, dataDate.length());
                        while (dataDate.toString().equals(date.toString())) {

                            tempValues.add(Double.parseDouble(data.get("value").toString()));
                            i++;
                            if (i > precipitationResults.length() - 1) {
                                break;
                            } else {
                                data = precipitationResults.getJSONObject(i);
                                dataDate = new StringBuilder(data.getString("date"));
                                dataDate.delete(dataDate.length() - 9, dataDate.length());
                            }
                        }

                        for (int i = 0; i < tempValues.size(); i++) {
                            sum = (double) (sum + tempValues.get(i));
                        }
                        average = ((double) sum / tempValues.size());
                        precipitationsPrev.add(average);
                        date = date.plusDays(1);
                        sum = 0;
                        tempValues = new ArrayList<>();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    sixthVolleyCall(precipitationsPrev);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("token", API_KEY);
                return params;
            }
        };
        queue.add(getPrecipitationPrev);

    }

    protected void sixthVolleyCall(List<Double> newPrecipitationsPrev) throws JSONException {
        precipitationsPrev = newPrecipitationsPrev;
        String minTempsUrlPrev = "https://www.ncdc.noaa.gov/cdo-web/api/v2/data?datasetid=GHCND&locationid="
                + location + "&startdate=" + startDatePrev.toString() + "T00:00:00&enddate=" + endDatePrev.toString() + "T00:00:00&datatypeid=TMIN&units=standard&limit=1000";

        JsonObjectRequest getMinTempsPrev = new JsonObjectRequest(Request.Method.GET, minTempsUrlPrev, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    responseString = response.toString();
                    JSONObject obj = new JSONObject(responseString);
                    minTempResults = obj.getJSONArray("results");
                    date = startDatePrev;
                    JSONObject data = minTempResults.getJSONObject(0);
                    i = 0;
                    while (!date.toString().equals(endDatePrev.plusDays(1).toString())) {

                        StringBuilder dataDate = new StringBuilder(data.getString("date"));
                        dataDate.delete(dataDate.length() - 9, dataDate.length());
                        while (dataDate.toString().equals(date.toString())) {

                            tempValues.add(Double.parseDouble(data.get("value").toString()));
                            i++;
                            if (i > minTempResults.length() - 1) {
                                break;
                            } else {
                                data = minTempResults.getJSONObject(i);
                                dataDate = new StringBuilder(data.getString("date"));
                                dataDate.delete(dataDate.length() - 9, dataDate.length());
                            }
                        }

                        for (int i = 0; i < tempValues.size(); i++) {
                            sum = (double) (sum + tempValues.get(i));
                        }
                        average = ((double) sum / tempValues.size());
                        lowTempsPrev.add(average);
                        date = date.plusDays(1);
                        sum = 0;
                        tempValues = new ArrayList<>();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                onPostExecute(lowTempsPrev);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();

            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("token", API_KEY);
                return params;
            }
        };
        queue.add(getMinTempsPrev);
    }


    // ***any activity with the API data must happen within this method***
    // Sliding window implementation
    // Database population
    protected void onPostExecute(List<Double> newLowTempsPrev) {
        lowTempsPrev = newLowTempsPrev;
        List<Double> highTempsWindow1 = new ArrayList<>();
        List<Double> highTempsWindow2 = new ArrayList<>();
        List<Double> highTempsWindow3 = new ArrayList<>();
        List<Double> highTempsWindow4 = new ArrayList<>();
        List<Double> highTempsWindow5 = new ArrayList<>();
        List<Double> highTempsWindow6 = new ArrayList<>();
        List<Double> highTempsWindow7 = new ArrayList<>();
        List<Double> highTempsWindow8 = new ArrayList<>();
        List<Double> CDhightemps = highTemps;             // reassigning lists to improve readability for algorithm implementation
        List<Double> CDlowtemps = lowTemps;
        List<Double> CDprecipitation = precipitations;
        // the selected date is stored in the arrays for last year's data; removing it for accurate
        // algorithm implementation
        highTempsPrev.remove(7);
        lowTempsPrev.remove(7);
        precipitationsPrev.remove(7);

        // populate the database
        PopulateDatabase popdb = new PopulateDatabase(dates, datesPrevList, highTemps, lowTemps,
                precipitations, highTempsPrev, lowTempsPrev, precipitationsPrev);
        popdb.populateDatabase();

        //*** sliding window algorithm ***
        // create 8 sliding windows each for high temps
        // window 1
        for(int i = 0; i < 7; i++) {
            highTempsWindow1.add(highTempsPrev.get(i));
        }
        // window 2
        for(int i = 1; i < 8; i++) {
            highTempsWindow2.add(highTempsPrev.get(i));
        }
        // window 3
        for(int i = 2; i < 9; i++) {
            highTempsWindow3.add(highTempsPrev.get(i));
        }
        // window 4
        for(int i = 3; i < 10; i++) {
            highTempsWindow4.add(highTempsPrev.get(i));
        }
        // window 5
        for(int i = 4; i < 11; i++) {
            highTempsWindow5.add(highTempsPrev.get(i));
        }
        // window 6
        for(int i = 5; i < 12; i++) {
            highTempsWindow6.add(highTempsPrev.get(i));
        }
        // window 7
        for(int i = 6; i < 13; i++) {
            highTempsWindow7.add(highTempsPrev.get(i));
        }
        // window 8
        for(int i = 7; i < 14; i++) {
            highTempsWindow8.add(highTempsPrev.get(i));
        }

        // compute Euclidean distances with CD
        List<Double> difference1 = new ArrayList<>();
        List<Double> difference2 = new ArrayList<>();
        List<Double> difference3 = new ArrayList<>();
        List<Double> difference4 = new ArrayList<>();
        List<Double> difference5 = new ArrayList<>();
        List<Double> difference6 = new ArrayList<>();
        List<Double> difference7 = new ArrayList<>();
        List<Double> difference8 = new ArrayList<>();
        // step 1: take differences and store in list
        for(int i = 0; i < 7; i++) {
            difference1.add(highTempsWindow1.get(i) - CDhightemps.get(i));
            difference2.add(highTempsWindow2.get(i) - CDhightemps.get(i));
            difference3.add(highTempsWindow3.get(i) - CDhightemps.get(i));
            difference4.add(highTempsWindow4.get(i) - CDhightemps.get(i));
            difference5.add(highTempsWindow5.get(i) - CDhightemps.get(i));
            difference6.add(highTempsWindow6.get(i) - CDhightemps.get(i));
            difference7.add(highTempsWindow7.get(i) - CDhightemps.get(i));
            difference8.add(highTempsWindow8.get(i) - CDhightemps.get(i));
        }

        Log.d("difference1", difference1.toString());

        List<Double> square1 = new ArrayList<>();
        List<Double> square2 = new ArrayList<>();
        List<Double> square3 = new ArrayList<>();
        List<Double> square4 = new ArrayList<>();
        List<Double> square5 = new ArrayList<>();
        List<Double> square6 = new ArrayList<>();
        List<Double> square7 = new ArrayList<>();
        List<Double> square8 = new ArrayList<>();

        // square all values
        for(int i = 0; i < 7; i++) {
            square1.add(Math.pow(difference1.get(i), 2));
            square2.add(Math.pow(difference1.get(i), 2));
            square3.add(Math.pow(difference1.get(i), 2));
            square4.add(Math.pow(difference1.get(i), 2));
            square5.add(Math.pow(difference1.get(i), 2));
            square6.add(Math.pow(difference1.get(i), 2));
            square7.add(Math.pow(difference1.get(i), 2));
            square8.add(Math.pow(difference1.get(i), 2));
        }

        double sum1 = 0;
        double sum2 = 0;
        double sum3 = 0;
        double sum4 = 0;
        double sum5 = 0;
        double sum6 = 0;
        double sum7 = 0;
        double sum8 = 0;

        // add values together
        for(int i = 0; i < 7; i++) {
            if(!Double.isNaN(square1.get(i)))
                sum1 = sum1 + square1.get(i);
            if(!Double.isNaN(square2.get(i)))
                sum2 = sum2 + square2.get(i);
            if(!Double.isNaN(square3.get(i)))
                sum3 = sum3 + square3.get(i);
            if(!Double.isNaN(square4.get(i)))
                sum4 = sum4 + square4.get(i);
            if(!Double.isNaN(square5.get(i)))
                sum5 = sum5 + square5.get(i);
            if(!Double.isNaN(square6.get(i)))
                sum6 = sum6 + square6.get(i);
            if(!Double.isNaN(square7.get(i)))
                sum7 = sum7 + square7.get(i);
            if(!Double.isNaN(square1.get(i)))
                sum8 = sum8 + square1.get(i);
        }
        // take square root
        double[] ED = new double[8];
        ED[0] = Math.sqrt(sum1);
        ED[1] = Math.sqrt(sum2);
        ED[2] = Math.sqrt(sum3);
        ED[3] = Math.sqrt(sum4);
        ED[4] = Math.sqrt(sum5);
        ED[5] = Math.sqrt(sum6);
        ED[6] = Math.sqrt(sum7);
        ED[7] = Math.sqrt(sum8);

        // select corresponding matrix
        double smallestED = ED[0];
        int window = 1;
        for(int i = 1; i < 7; i++) {
            if(ED[i] < smallestED) {
                smallestED = ED[i];
                window = i + 1;
            }
        }

        List<Double> highTempWindow = new ArrayList<>();
        switch(window) {
            case 1:
                highTempWindow = highTempsWindow1;
                break;

            case 2:
                highTempWindow = highTempsWindow2;
                break;

            case 3:
                highTempWindow = highTempsWindow3;
                break;

            case 4:
                highTempWindow = highTempsWindow4;
                break;

            case 5:
                highTempWindow = highTempsWindow5;
                break;

            case 6:
                highTempWindow = highTempsWindow6;
                break;

            case 7:
                highTempWindow = highTempsWindow7;
                break;

            case 8:
                highTempWindow = highTempsWindow8;
                break;
        }

        List<Double> WCCD = new ArrayList<>();
        List<Double> WCVP = new ArrayList<>();
        int numToSubtrFromMean = 0;
        for(int i = 0; i < 7; i++) {
            if(!Double.isNaN(highTemps.get(i))) {
                WCCD.add(Math.abs(smallestED - CDhightemps.get(i)));
                numToSubtrFromMean++;
            }

            WCVP.add(Math.abs(smallestED - highTempWindow.get(i)));
        }

        double count = 7 - numToSubtrFromMean;
        double mean1 = 0;
        double mean2 = 0;

        for(double i : WCCD) {
            mean1 = mean1 + i;
        }
        for(double i : WCVP) {
            mean2 = mean2 + i;
        }

        mean1 = (mean1 / count);
        mean2 = (mean2 / 7);

        double predictedVarHighTemp = (mean1 + mean2) / 2; // FIXME: variation is very high?

        Log.d("predictedVarHighTemp", "" + predictedVarHighTemp);







    }

}
