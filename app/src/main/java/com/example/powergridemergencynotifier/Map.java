package com.example.powergridemergencynotifier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

// Implement OnMapReadyCallback.
public class Map extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    double map_lat=0.00;
    double map_lon=0.00;
    GoogleMap mMap;
    LatLng area;
    boolean doubleBackToExitPressedOnce = false;
    Object MarkerCheck1="m", MarkerCheck2;
    Geocoder geocoder;
    List<Address> addresses;
    String time;
    String category;
    DatabaseReference alertRef;
    String cityCol, typeCol, UIDCol, categoryCol, dateCol, latLong, latVal, longVal;
    float alphaSet;
    DatabaseReference firebaseDatabase;
    List<Double> latAndLon = new ArrayList<Double>();
    List<String> typeList = new ArrayList<String>();
    List<String> UIDList = new ArrayList<String>();
    List<String> categoryList = new ArrayList<String>();
    List<String> dateList = new ArrayList<String>();
    static final float COORDINATE_OFFSET = 0.005f;
    List<String> markerLocation = new ArrayList<String>();
    double newLat, newLong;
    private HashMap<Marker, Integer> markerIdMapping = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout file as the content view.
        setContentView(R.layout.map);

        //allows back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        alertRef = firebaseDatabase.child("Alerts");

        //calling stored lat and lon
        SharedPreferences coordinates = PreferenceManager.getDefaultSharedPreferences(this);
        String str_map_lat = coordinates.getString("lat", "");
        String str_map_lon = coordinates.getString("lon", "");

        //convert lat and lon to double
        map_lat = Double.parseDouble(str_map_lat);
        map_lon = Double.parseDouble(str_map_lon);

        // Get a handle to the fragment and register the callback.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //back button
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    // Get a handle to the GoogleMap object and display marker.
    @Override
    public void onMapReady(GoogleMap googleMap) {

        googleMap.setOnMarkerClickListener(this);

        mMap = googleMap;
        mMap.clear();
        //center map around enter lat and lon
        area = new LatLng(map_lat, map_lon);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(area));

        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                    UIDCol = dsp.getKey().toString(); //gets UID

                    cityCol = dsp.child("City").getValue(String.class); //gets city
                    typeCol = dsp.child("Type").getValue(String.class); //gets type
                    categoryCol = dsp.child("Category").getValue(String.class); //gets category
                    dateCol = dsp.child("Date").getValue(String.class); //gets date
                    if(Geocoder.isPresent()){
                        try {
                            Geocoder gc = new Geocoder(getApplicationContext());
                            List<Address> addresses= gc.getFromLocationName(cityCol, 5); // get the found Address Objects
                            //Log.i("websocketclient",cityCol);
                            for(Address a : addresses){
                                if(a.hasLatitude() && a.hasLongitude()){
                                    latVal = String.valueOf(a.getLatitude());
                                    longVal = String.valueOf(a.getLongitude());
                                    latLong = latVal + " " + longVal;
                                    Log.i("test", String.valueOf(markerLocation));
                                    while (markerLocation.contains(latLong)){
                                        Log.i("while", "in while");
                                        String [] tokens = latLong.split(" ");
                                        newLat = Double.parseDouble(tokens[0])+COORDINATE_OFFSET;
                                        newLong = Double.parseDouble(tokens[1])+COORDINATE_OFFSET;
                                        latVal = String.valueOf(newLat);
                                        longVal = String.valueOf(newLong);
                                        latLong = latVal + " " + longVal;
                                    }
                                    markerLocation.add(latLong);
                                    String[] token = latLong.split(" ");
                                    double latitudeVal = Double.parseDouble(token[0]);
                                    double longitudeVal = Double.parseDouble(token[1]);

                                    //store lat and long in array
                                    latAndLon.add(latitudeVal);
                                    latAndLon.add(longitudeVal);
                                    //store type of hazard in array
                                    typeList.add(typeCol);
                                    typeList.add(" ");
                                    //store category of hazard in array
                                    categoryList.add(categoryCol);
                                    categoryList.add(" ");
                                    //store date of hazard in array
                                    dateList.add(dateCol);
                                    dateList.add(" ");
                                    //store UID of hazard in array
                                    UIDList.add(UIDCol);
                                    UIDList.add(" ");
                                }
                            }AddingNewMark();
                        } catch (IOException e) {
                            // handle the exception
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
            alertRef.addListenerForSingleValueEvent(eventListener);
    }
    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();  //show title of marker
        SharedPreferences notif_page = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = notif_page.edit();
        //get UID from marker id and array
        int markerID = markerIdMapping.get(marker);
        //store UID to be used on notif page
        editor.putString("UID", UIDList.get(markerID));
        editor.apply();
        //open notif page
        Intent intent = new Intent(this, NotificationPage.class);
        startActivity(intent);
        return true;

    }
    private void AddingNewMark(){
        //Toast.makeText(Map.this,latAndLon.toString(), Toast.LENGTH_LONG).show();
        for (int j = 0; j < (typeList.size()); j++){
            //gets current date
            String currentDate = new SimpleDateFormat("MM/dd", Locale.getDefault()).format(new Date());
            if (dateList.get(j).equals(currentDate)){
                //if hazard is current date then set to full opacity
                alphaSet = 1f;
            }else{
                //if not current date set lower opacity
                alphaSet = 0.2f;
            }
            Log.i("latandLong", String.valueOf(latAndLon));
            //adding marker
            Marker marker = mMap.addMarker(new MarkerOptions()
                    //get lat and long stored in array
                            .position(new LatLng(latAndLon.get(j), latAndLon.get(j+1)))
                    //get type of hazard store in array for title
                            .title(typeList.get(j)));
                    //set opacity according to date
                            //.alpha(alphaSet));
            //set id of marker to unique code to retrieve UID from array
            markerIdMapping.put(marker,j);
            //color of marker to specify if current date or not
            if (!dateList.get(j).equals(currentDate)) {
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            }
            j++;
        }
    }

}
