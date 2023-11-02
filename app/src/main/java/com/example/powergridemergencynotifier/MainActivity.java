package com.example.powergridemergencynotifier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import tech.gusavila92.websocketclient.WebSocketClient;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    boolean isAvailable;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference;
    private WebSocketClient client;
    String userEmail;
    String userZipcode;
    String status, UIN;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState); //tells to run all the code
        setContentView(R.layout.activity_main); //xml file connected to class

        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference();

        Button button1 = findViewById(R.id.button2);
        button1.setOnClickListener(this);  //listener for submit button

        //if any stored zipcode from user open different page
        SharedPreferences coordinates = PreferenceManager.getDefaultSharedPreferences(this);
        String value = coordinates.getString("zipcode", ""); //check zipcode storage
        SharedPreferences login = PreferenceManager.getDefaultSharedPreferences(this);
        String check = login.getString("email", ""); //check email storage

        if(value!=null && !value.equals("")){ //if not empty
            //Re-Direct to new home screen
            Intent intent0 = new Intent(this, Options.class);
            startActivity(intent0);
        }

        if(check==null || check.equals("")){ //if empty
            //Re-Direct to new home screen
            Intent intent0 = new Intent(this, Login.class);
            startActivity(intent0);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button2: //submit button pressed
                final EditText zip_in =  findViewById(R.id.zipcode); //allow user to enter zipcode
                String zipcode = zip_in.getText().toString();  //convert zipcode to string
                if (zipcode.length() == 5){ //checking zip code is appropriate length
                    if (isNetworkOnline2()) { //check network is online
                        // Execution here
                        final Geocoder geocoder = new Geocoder(this);
                        try {
                            //Toast.makeText(MainActivity.this,zipcode,Toast.LENGTH_LONG).show();
                            List<Address> addresses = geocoder.getFromLocationName(zipcode, 5);
                            //Toast.makeText(MainActivity.this,addresses.toString(),Toast.LENGTH_LONG).show();
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                // Use the address as needed
                                Toast.makeText(MainActivity.this,zipcode.toString(),Toast.LENGTH_LONG).show();
                                double main_lat = address.getLatitude(); //conv to lat
                                double main_lon = address.getLongitude(); //conv to long

                                //convert double to string
                                String string_lat = String.valueOf(main_lat);
                                String string_lon = String.valueOf(main_lon);

                                //store lat, long, and zipcode for future use
                                SharedPreferences coordinates = PreferenceManager.getDefaultSharedPreferences(this);
                                SharedPreferences.Editor editor = coordinates.edit();
                                editor.putString("lat", string_lat);
                                editor.putString("lon", string_lon);
                                editor.putString("zipcode", zipcode);
                                editor.apply();

                                SharedPreferences login = PreferenceManager.getDefaultSharedPreferences(this);
                                userEmail = login.getString("email", ""); //retrieve email
                                userZipcode = login.getString("zipcode",""); //retrieve zipcode

                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("email", userEmail);
                                hashMap.put("zipcode", userZipcode);

                                UIN = userEmail.replace(".", "");
                                UIN = UIN.replace("#", "");
                                UIN = UIN.replace("$", "");
                                UIN = UIN.replace("/","");
                                UIN = UIN.replace("[","");
                                UIN = UIN.replace("]","");


                                //check if user already has zipcode stored
                                DatabaseReference userNameRef = reference.child("User").child(UIN);
                                ValueEventListener eventListener = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if(!dataSnapshot.exists()) {
                                            //store to database
                                            //new user, new zipcode
                                            status = "new user";
                                            reference.child("User")
                                                    .child(UIN) //find label to save each user under, cannot use email
                                                    .setValue(hashMap)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            Toast.makeText(MainActivity.this, "Successfully saved to database", Toast.LENGTH_LONG).show();
                                                            //createWebSocketClient();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(MainActivity.this, "Failed to save to database", Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                        }else{
                                            //existing user, new zipcode
                                            status = "exist user";
                                            userNameRef.setValue(hashMap);
                                            //createWebSocketClient();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        //error
                                        Toast.makeText(MainActivity.this, "error",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                };
                                userNameRef.addListenerForSingleValueEvent(eventListener);

                                //open next page
                                Intent intent1 = new Intent(this, Options.class);
                                startActivity(intent1);

                            } else {
                                // Display appropriate message when Geocoder services are not available
                                Toast.makeText(this, "Invalid zipcode", Toast.LENGTH_LONG).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            // handle exception
                        }
                        break;
                    }

                    else {
                        // Error message here if network is unavailable.
                        Toast.makeText(this, "Network is unavailable!", Toast.LENGTH_LONG).show();
                    }

                }
                //zipcode is not correct length
                else{
                    Toast.makeText(MainActivity.this, "need 5 digit zipcode", Toast.LENGTH_LONG).show();
                    break;
                }
            default:
                break;
        }
    }
    //not using anymore
    private void createWebSocketClient() {
        URI uri;
        try {
            // Connect to local host
            uri = new URI("ws://10.229.167.183:12345/websocket");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        client = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                //send email to server
                Log.i("WebSocket","Session is starting");
                Log.i("WebSocket",status);
                //client.send(userEmail + ":" + userZipcode);
                if (status.equals("new user")) {
                    //client.send("Add User:" + userEmail + ":" + userZipcode);


                } else if (status.equals("exist user")) {
                    //client.send("Update Zipcode:" + userEmail + ":" + userZipcode);


                }
            }

            @Override
            public void onTextReceived(String message) {
                Log.i("websocketclient",message);

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

            }
        };
        /*client.setConnectTimeout(10000);
        client.setReadTimeout(60000);
        client.enableAutomaticReconnection(5000);*/
        client.connect();
    }

}
