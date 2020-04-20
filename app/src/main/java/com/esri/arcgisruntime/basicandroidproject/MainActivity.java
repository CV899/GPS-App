package com.esri.arcgisruntime.basicandroidproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;

import android.content.Intent;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    public static Context context;
    private MapView mMapView;
    private LocationDisplay mLocationDisplay;
    FusedLocationProviderClient locationClient;
    double latitude = 34.0270; // latitude and longitude are overwritten if location services are on
    double longitude = -118.8050;
    EditText location;
    Button submitButton;
    Button GPS_button;
    String state;
    InputStream iS;
    BufferedReader reader = null;
    String json;
    ArrayList<String> cities;
    ArrayList<String> IDs;
    Spinner dropDown;
    int listPosition;
    DatePicker selectedDate;
    String date;
    List<String> highTemps = new ArrayList<>();
    List<String> lowTemps = new ArrayList<>();
    List<String> humidity = new ArrayList<>();

    private void setupMap() {
        if (mMapView != null) {
            ArcGISRuntimeEnvironment.setLicense(getResources().getString(R.string.arcgis_license_key));
            Basemap.Type basemapType = Basemap.Type.STREETS_VECTOR;
            int levelOfDetail = 13;
            ArcGISMap map = new ArcGISMap(basemapType, latitude, longitude, levelOfDetail);
            //mMapView.setMap(map);
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = findViewById(R.id.mapView);
        submitButton = (Button) findViewById(R.id.submitButton);
        GPS_button = (Button) findViewById(R.id.GPS_button);
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        dropDown = (Spinner) findViewById(R.id.spinner);

        setupMap();
        setupLocationDisplay();
        getLastLocation();
        getAddress();

        try {
            createAllLocationsList();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, cities);
        dropDown.setAdapter(adapter);
        dropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                listPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        GPS_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLastLocation();
                getAddress();
                try {
                    createStateLocationsList();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                adapter.clear();
                adapter.addAll(cities);
                adapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Drop down list has been updated!", Toast.LENGTH_LONG).show();
            }
        });


        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDate = (DatePicker) findViewById(R.id.selectDate);
                date = selectedDate.getYear() + "-" + (selectedDate.getMonth() + 1) + "-" + selectedDate.getDayOfMonth();
                Intent launchActivity1 = new Intent(MainActivity.this, Results.class);
                launchActivity1.putExtra("location", IDs.get(listPosition).toString());
                launchActivity1.putExtra("date", date);
                startActivity(launchActivity1);
            }
        });
    }

    public void createAllLocationsList() throws JSONException, IOException {
        iS = getAssets().open("cities.json");
        int size = iS.available();
        byte[] buffer = new byte[size];
        iS.read(buffer);
        iS.close();
        json = new String(buffer, "UTF-8");
        JSONObject jsonObj = new JSONObject(json);
        JSONArray jsonArr = jsonObj.getJSONArray("results");

        cities = new ArrayList<>();
        IDs = new ArrayList<>();
        for(int i = 0; i < jsonArr.length() - 1; i++) {
            JSONObject names = jsonArr.getJSONObject(i);
            String name = (String) names.get("name");
            String ID = (String) names.get("id");
            cities.add(name);
            IDs.add(ID);
        }

    }

    public void createStateLocationsList() throws IOException, JSONException {
        iS = getAssets().open("cities.json");
        int size = iS.available();
        byte[] buffer = new byte[size];
        iS.read(buffer);
        iS.close();
        json = new String(buffer, "UTF-8");
        JSONObject jsonObj = new JSONObject(json);
        JSONArray jsonArr = jsonObj.getJSONArray("results");

        cities = new ArrayList<>();
        IDs = new ArrayList<>();
        for(int i = 0; i < jsonArr.length() - 1; i++) {
            JSONObject names = jsonArr.getJSONObject(i);
            String name = (String) names.get("name");
            String ID = (String) names.get("id");
            if(name.contains(", " + state)) {
                cities.add(name);
                IDs.add(ID);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        MenuItem syncMenuItem = menu.findItem(R.id.sync);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.sync)
            getAddress();
        return true;
    }

    private void getAddress() {
        //Log.d("GPS", String.format("X: %f, Y: %f", longitude, latitude));
        String url = String.format(Locale.US, "http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode?location=%f,%f",longitude,latitude);
        //Log.d("rGEO", url);

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Res", response);
                String[] split = response.split("LongLabel", 2); // returns the US state from the address
                String[] trim = split[1].split("<br/>", 2);
                String addr = trim[0].substring(6);
                String[] addrSplit = addr.split(", ");
                Log.d("State", addrSplit[2]);  //addrSplit[2] is the abbreviated state
                state = addrSplit[2];
                //Toast.makeText(MainActivity.this, addrSplit[2], Toast.LENGTH_LONG).show();
                queue.stop();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = "Error in Volley StringRequest";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                queue.stop();
            }
        });

        queue.add(stringRequest);

    }

    private void getLastLocation() {
        locationClient.getLastLocation().addOnCompleteListener(
                new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(Task<Location> task) {
                        Location location = task.getResult();
                        longitude = location.getLongitude();
                        latitude = location.getLatitude();

                    }
                }
        );
    }

    @Override
    protected void onPause() {
        if (mMapView != null) {
            mMapView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null) {
            mMapView.dispose();
        }
        super.onDestroy();
    }

    private void setupLocationDisplay() {
        mLocationDisplay = mMapView.getLocationDisplay();
        mLocationDisplay.addDataSourceStatusChangedListener(dataSourceStatusChangedEvent -> {
            if (dataSourceStatusChangedEvent.isStarted() || dataSourceStatusChangedEvent.getError() == null) {
                return;
            }

            int requestPermissionsCode = 2;
            String[] requestPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

            if (!(ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[0]) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[1]) == PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(MainActivity.this, requestPermissions, requestPermissionsCode);
            } else {
                String message = String.format("Error in DataSourceStatusChangedListener: %s",
                        dataSourceStatusChangedEvent.getSource().getLocationDataSource().getError().getMessage());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        mLocationDisplay.startAsync();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationDisplay.startAsync();
        } else {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }
}
