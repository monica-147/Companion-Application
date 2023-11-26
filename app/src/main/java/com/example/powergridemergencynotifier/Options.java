package com.example.powergridemergencynotifier;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
// import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tech.gusavila92.websocketclient.WebSocketClient;

public class Options extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener{

    private WebSocketClient webSocketClient;
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    public NavigationView navigationView;
    DatabaseReference firebaseDatabase, alertRef;
    TableLayout tl;
    TableRow tr_head;
    String dateCol, typeCol, categoryCol, cityCol, messageType, threatVal;
    JSONArray info_list, infoArray;

    String dateTime, messageContent, threat_level, city, UID, currentDate, user, zipcode;
    HashMap<String, Object> hashMap = new HashMap<>();
    List<String> UIDThreat = new ArrayList<String>();

    List<String> UIDremaining = new ArrayList<String>();

    private void setNavigationViewListener() {
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options);
        setNavigationViewListener();  //listener for nav menu

        currentDate = new SimpleDateFormat("MM/dd", Locale.getDefault()).format(new Date());

        Button button1 = findViewById(R.id.button2);
        button1.setOnClickListener(this);  //listener for mapview button

        tl = (TableLayout) findViewById(R.id.tableLayout);
        tl.removeViews(1,Math.max(0, tl.getChildCount() - 1));

        if (isNetworkOnline2()) { //check network is online

            firebaseDatabase = FirebaseDatabase.getInstance().getReference();
            alertRef = firebaseDatabase.child("Alerts");

            createWebSocketClient();

            // drawer layout instance to toggle the menu icon to open
            // drawer and back button to close drawer
            drawerLayout = findViewById(R.id.my_drawer_layout);
            actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);

            // pass the Open and Close toggle for the drawer layout listener
            // to toggle the button
            drawerLayout.addDrawerListener(actionBarDrawerToggle);
            actionBarDrawerToggle.syncState();

            // to make the Navigation drawer icon always appear on the action bar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            PopulateTable();
        }
    }
    public static boolean isNetworkOnline2() {
        boolean isOnline = false;
        try {
            Runtime runtime = Runtime.getRuntime();
            Process p = runtime.exec("ping -c 1 8.8.8.8");
            int waitFor = p.waitFor();
            isOnline = waitFor == 0;    // only when the waitFor value is zero, the network is online indeed

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return isOnline;
    }

    // override the onOptionsItemSelected()
    // function to implement
    // the item click listener callback
    // to open and close the navigation
    // drawer when the icon is clicked
    @Override
    public void onClick( View view ) {
        switch(view.getId()) {
            case R.id.button2: {//mapView button pressed
                Intent intent2 = new Intent(this, Map.class);
                startActivity(intent2);
                break;
        }}
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            //allow nav menu to toggle
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {

            case R.id.newzip: {
                SharedPreferences credentials = PreferenceManager.getDefaultSharedPreferences(this);
                //retrieving stored user values
                String oldZip = credentials.getString("zipcode", "");
                String oldLat = credentials.getString("lat", "");
                String oldLong = credentials.getString("lon", "");

                SharedPreferences coordinates = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = coordinates.edit();
                //clear lat and long for new input
                editor.putString("lat", "");
                editor.putString("lon", "");
                editor.putString("zipcode", "");
                editor.putString("oldlat", oldLat);
                editor.putString("oldlon", oldLong);
                editor.putString("oldzipcode", oldZip);
                zipcode = "";
                editor.apply();
                alertRef.removeValue();
                tl.removeViews(1,Math.max(0, tl.getChildCount() - 1));
                //open main activity page
                Intent intent1 = new Intent(this, MainActivity.class);
                startActivity(intent1);
                break;
            }
            case R.id.contact: {
                //open help page
                Intent intent3 = new Intent(this, Help.class);
                startActivity(intent3);
                break;
            }
            case R.id.signout: {
                //clear login  and zipcode for new input
                SharedPreferences login = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = login.edit();
                editor.putString("email", "");
                editor.putString("password", "");
                editor.putString("lat", "");
                editor.putString("lon", "");
                editor.putString("zipcode", "");
                zipcode = "";
                editor.apply();
                alertRef.removeValue();
                tl.removeViews(1,Math.max(0, tl.getChildCount() - 1));
                //open login page
                Intent intent4 = new Intent(this, Login.class);
                startActivity(intent4);
            }
        }
        //close navigation drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    private void createWebSocketClient() {
        URI uri;
        try {
            // Connect to local host
            uri = new URI("ws://10.254.202.63:12345/websocket");
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        SharedPreferences credentials = PreferenceManager.getDefaultSharedPreferences(this);
        //retrieving stored user values
        user = credentials.getString("email", "");
        zipcode = credentials.getString("zipcode", "");

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                //send email to server
                webSocketClient.send(user+":"+zipcode+":get history");
            }
            @Override
            public void onTextReceived(String message) {
                Log.i("websocketclient",message);
                try{
                    if (message.startsWith("[{")){
                        JSONArray updates = new JSONArray(message);
                        for (int i = 0; i < updates.length(); i++) {
                            JSONObject update = updates.getJSONObject(i);
                            String type = update.getString("type");
                            infoArray = update.getJSONArray("info");

                            //Log.i("ur mom",infoArray.toString());
                            if (infoArray.length() == 0) {
                                Log.i("websocketclient","no threats");
                            }for (int j = 0; j < infoArray.length(); j++) {
                                Log.d("ur mom", String.valueOf(j));
                                //get array within array
                                info_list = infoArray.getJSONArray(j);
                                //make message lowercase
                                String lower = info_list.getString(2).toLowerCase();
                                //check keywords to find time
                                if (lower.contains("ablaze") || lower.contains("fire") || lower.contains("burned")
                                        || lower.contains("burning") || lower.contains("flare")){
                                    messageType = "Fire";
                                } else if (lower.contains("power") || lower.contains("outage") || lower.contains("transformer")){
                                    messageType = "Power";
                                } else if (lower.contains("flood") || lower.contains("hurricane") || lower.contains("tsunami")
                                        || lower.contains("cyclone")){
                                    messageType = "Flood";
                                } else if (lower.contains("blizzard") || lower.contains("ice") || lower.contains("icing")){
                                    messageType = "Ice";
                                } else if (lower.contains("storm") || lower.contains("lightning") || lower.contains("wind")){
                                    messageType = "Storm";
                                } else if (lower.contains("debris") || lower.contains("collapse") || lower.contains("danger")
                                        || lower.contains("shelter")){
                                    messageType = "Danger";
                                } else if (lower.contains("tornado")){
                                    messageType = "Tornado";
                                } else if (lower.contains("earthquake")){
                                    messageType = "Earthquake";
                                }
                                UID = info_list.getString(0).substring(0,5).replace("/"," ")+" "+info_list.getString(3)+" "+messageType;

                                //sent message and details to database
                                dateTime = info_list.getString(0);
                                threat_level = info_list.getString(1);
                                messageContent = info_list.getString(2);
                                city = info_list.getString(3);

                                ValueEventListener eventListener = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        //adding messages to database using their UID
                                        /*String timeMess = dateTime.substring(12, 17) + messageContent;
                                        //HashMap<String, Object> hashMap = new HashMap<>();
                                        hashMap.put("Date", dateTime.substring(0, 5));
                                        if (threat_level.equals("Immediate")) {
                                            hashMap.put("Category", "Immediate");
                                        }
                                        hashMap.put("Category", threat_level);
                                        hashMap.put("Type", messageType);
                                        hashMap.put("City", city);
                                        String timeLabel = timeMess.replace(".", "");
                                        hashMap.put(timeLabel, timeMess);
                                        Log.i("hash output",hashMap.toString());
                                        Log.i("UID", UID);
                                        alertRef.child(UID).updateChildren(hashMap);*/
                                    }@Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        //Toast.makeText(Options.this,"cancelled",Toast.LENGTH_LONG).show();
                                    }
                                };
                                alertRef.child(UID).addListenerForSingleValueEvent(eventListener);
                                //UpdateDatabase();
                                //PopulateTable();
                                String timeMess = dateTime.substring(12, 17) + messageContent;
                                //HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("Date", dateTime.substring(0, 5));
                                //threatVal = "Immediate";
                                if (threat_level.equals("Immediate")) {
                                    UIDThreat.add(UID);
                                }
                                if (UIDThreat.contains(UID)){
                                    threatVal = "Immediate";
                                }else{
                                    threatVal = "Moderate";
                                }
                                hashMap.put("Category", threatVal);
                                hashMap.put("Type", messageType);
                                hashMap.put("City", city);
                                String timeLabel = timeMess.replace(".", "");
                                timeLabel = timeLabel.replace("/","");
                                timeLabel = timeLabel.replace("$","");
                                timeLabel = timeLabel.replace("#","");
                                timeLabel = timeLabel.replace("[","");
                                timeLabel = timeLabel.replace("]","");
                                hashMap.put(timeLabel, timeMess);
                                Log.i("hash output",hashMap.toString());
                                Log.i("UID", UID);
                                alertRef.child(UID).updateChildren(hashMap);
                                hashMap.clear();

                                //System.out.println();
                                //refresh(10000);
                            }
                        }
                    }
                    //format message and apply as needed
                } catch (Exception e){
                    e.printStackTrace();
                }
                //refresh(3000);

                //webSocketClient.send(user+":"+zipcode+":get history");
            }


            @Override
            public void onBinaryReceived(byte[] data) {
            }

            @Override
            public void onPingReceived(byte[] data) {
            }

            @Override
            public void onPongReceived(byte[] data) {
            }

            @Override
            public void onException(Exception e) {
                System.out.println(e.getMessage());
            }

            @Override
            public void onCloseReceived() {
                System.out.println("onCloseReceived");
            }
        };
        /*webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);*/
        webSocketClient.enableAutomaticReconnection(3000);
        webSocketClient.connect();
    }
    private void PopulateTable(){
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot dsp : dataSnapshot.getChildren()){
                    String getUID = dsp.getKey().toString(); //gets UID
                    if (!getUID.substring(0,5).replace(" ","/").equals(currentDate)){
                        UIDremaining.add(getUID);
                        //Log.i("UID", getUID.substring(0,5).replace(" ","/"));
                        //Log.i("adding", String.valueOf(!getUID.substring(0,4).equals(currentDate)));
                    }else {
                        Log.i("UID", getUID.substring(0, 5).replace(" ", "/"));
                        dateCol = dataSnapshot.child(getUID).child("Date").getValue(String.class); //gets date
                        typeCol = dsp.child("Type").getValue(String.class); //gets type
                        categoryCol = dsp.child("Category").getValue(String.class); //gets category
                        cityCol = dsp.child("City").getValue(String.class); //gets city

                        tr_head = new TableRow(getApplicationContext());
                        tl.addView(tr_head, new TableLayout.LayoutParams());

                        tr_head.setTag(getUID);
                        tr_head.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //communicate UID
                                SharedPreferences notif_page = PreferenceManager.getDefaultSharedPreferences(Options.this);
                                //store data to use on notif page
                                SharedPreferences.Editor editor = notif_page.edit();
                                editor.putString("UID", view.getTag().toString());
                                editor.apply();
                                //open notification page
                                Intent intent3 = new Intent(Options.this, NotificationPage.class);
                                startActivity(intent3);
                            }
                        });
                        TextView labelDate = new TextView(Options.this);
                        labelDate.setTextColor(Color.RED);
                        labelDate.setText(dateCol); //set text for first col, date
                        labelDate.setWidth(0);
                        labelDate.setGravity(Gravity.CENTER);
                        labelDate.setTextSize(14);
                        labelDate.setPadding(18, 18, 18, 18);
                        tr_head.addView(labelDate);// add the column to the table row here

                        TextView labelCategory = new TextView(Options.this);
                        labelCategory.setTextColor(Color.RED);
                        labelCategory.setText(categoryCol); // set the text for second col, category
                        labelCategory.setGravity(Gravity.CENTER);
                        labelCategory.setWidth(0);
                        labelCategory.setTextSize(14);
                        labelCategory.setPadding(5, 5, 5, 5);
                        tr_head.addView(labelCategory); // add the column to the table row here

                        TextView labelType = new TextView(Options.this);
                        labelType.setTextColor(Color.RED);
                        labelType.setText(typeCol); // set the text for third col, type
                        labelType.setGravity(Gravity.CENTER);
                        labelType.setWidth(0);
                        labelType.setTextSize(14);
                        labelType.setPadding(5, 5, 5, 5);
                        tr_head.addView(labelType);// add the column to the table row here

                        TextView labelCity = new TextView(Options.this);
                        labelCity.setTextColor(Color.RED);
                        labelCity.setText(cityCol); // set the text for fourth col, city
                        labelCity.setGravity(Gravity.CENTER);
                        labelCity.setWidth(0);
                        labelCity.setTextSize(14);
                        labelCity.setPadding(5, 5, 5, 5);
                        tr_head.addView(labelCity); // add the column to the table row here
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Options.this, "cancelled", Toast.LENGTH_LONG).show();
            }
        };
        alertRef.addListenerForSingleValueEvent(eventListener);
        //Log.i("UIDs", UIDremaining.toString());
        refresh(5000);
    }
    private void PopulateAgain(){
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                        for (int i = 0; i < UIDremaining.size(); i++) {
                            dateCol = dataSnapshot.child(UIDremaining.get(i)).child("Date").getValue(String.class); //gets date
                            typeCol = dataSnapshot.child(UIDremaining.get(i)).child("Type").getValue(String.class); //gets type
                            categoryCol = dataSnapshot.child(UIDremaining.get(i)).child("Category").getValue(String.class); //gets category
                            cityCol = dataSnapshot.child(UIDremaining.get(i)).child("City").getValue(String.class); //gets city

                            tr_head = new TableRow(getApplicationContext());
                            tl.addView(tr_head, new TableLayout.LayoutParams());

                            tr_head.setTag(UIDremaining.get(i));
                            tr_head.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //communicate UID
                                    SharedPreferences notif_page = PreferenceManager.getDefaultSharedPreferences(Options.this);
                                    //store data to use on notif page
                                    SharedPreferences.Editor editor = notif_page.edit();
                                    editor.putString("UID", view.getTag().toString());
                                    editor.apply();
                                    //open notification page
                                    Intent intent3 = new Intent(Options.this, NotificationPage.class);
                                    startActivity(intent3);
                                }
                            });
                            TextView labelDate = new TextView(Options.this);
                            labelDate.setText(dateCol); //set text for first col, date
                            labelDate.setWidth(0);
                            labelDate.setGravity(Gravity.CENTER);
                            labelDate.setTextSize(14);
                            labelDate.setPadding(18, 18, 18, 18);
                            tr_head.addView(labelDate);// add the column to the table row here

                            TextView labelCategory = new TextView(Options.this);
                            labelCategory.setText(categoryCol); // set the text for second col, category
                            labelCategory.setGravity(Gravity.CENTER);
                            labelCategory.setWidth(0);
                            labelCategory.setTextSize(14);
                            labelCategory.setPadding(5, 5, 5, 5);
                            tr_head.addView(labelCategory); // add the column to the table row here

                            TextView labelType = new TextView(Options.this);
                            labelType.setText(typeCol); // set the text for third col, type
                            labelType.setGravity(Gravity.CENTER);
                            labelType.setWidth(0);
                            labelType.setTextSize(14);
                            labelType.setPadding(5, 5, 5, 5);
                            tr_head.addView(labelType);// add the column to the table row here

                            TextView labelCity = new TextView(Options.this);
                            labelCity.setText(cityCol); // set the text for fourth col, city
                            labelCity.setGravity(Gravity.CENTER);
                            labelCity.setWidth(0);
                            labelCity.setTextSize(14);
                            labelCity.setPadding(5, 5, 5, 5);
                            tr_head.addView(labelCity); // add the column to the table row here
                        }
                        }
                        @Override
                        public void onCancelled (@NonNull DatabaseError error){
                            Toast.makeText(Options.this, "cancelled", Toast.LENGTH_LONG).show();
                        }
                    } ;
                    alertRef.addListenerForSingleValueEvent(eventListener);
                    UIDremaining.clear();
                }
    private void refresh (int milliseconds){
        final Handler handler = new Handler();

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                tl.removeViews(1,Math.max(0, tl.getChildCount() - 1));
                PopulateTable();
                PopulateAgain();
                webSocketClient.send(user+":"+zipcode+":get history");
                //createWebSocketClient();
            }
        };

        handler.postDelayed(runnable, milliseconds);
    }

}
