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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
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
    private WebSocketClient client;
    FirebaseDatabase Firebasedatabase;
    DatabaseReference reference;
    DatabaseReference firebaseDatabase, alertRef;
    String userEmail, retZip;
    String userZipcode;
    String status, UIN, backZip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); //tells to run all the code
        setContentView(R.layout.activity_main); //xml file connected to class

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Firebasedatabase = FirebaseDatabase.getInstance();
        reference = Firebasedatabase.getReference();

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
        String emailUIN = check.replace(".",",");
        emailUIN = emailUIN.replace("#","(");
        emailUIN = emailUIN.replace("$",")");
        emailUIN = emailUIN.replace("/","-");
        emailUIN = emailUIN.replace("[","+");
        emailUIN = emailUIN.replace("]","*");
        emailUIN = emailUIN.toLowerCase();
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        alertRef = firebaseDatabase.child(emailUIN);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //back button
            case android.R.id.home:
                BackButton();
                if (backZip.equals("yes")) {
                    Intent intent1 = new Intent(this, Options.class);
                    startActivity(intent1);
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public void BackButton(){
                    //store in shared preferences
                        if (isNetworkOnline2()) { //check network is online
                            // Execution here
                            SharedPreferences credentials = PreferenceManager.getDefaultSharedPreferences(this);
                            //retrieving stored user values
                            String oldZip = credentials.getString("oldzipcode", "");
                            String oldLat = credentials.getString("oldlat", "");
                            String oldLong = credentials.getString("oldlon", "");

                            if (oldLong.equals("") || oldLat.equals("") || oldZip.equals("")){
                                backZip = "no";
                                Toast.makeText(MainActivity.this, "Please enter ZipCode", Toast.LENGTH_LONG).show();
                            }else {
                                backZip = "yes";

                                SharedPreferences coordinates = PreferenceManager.getDefaultSharedPreferences(this);
                                SharedPreferences.Editor editor = coordinates.edit();
                                //clear lat and long for new input
                                editor.putString("lat", oldLat);
                                editor.putString("lon", oldLong);
                                editor.putString("zipcode", oldZip);
                                editor.putString("oldlat", "");
                                editor.putString("oldlon", "");
                                editor.putString("oldzipcode", "");
                                editor.apply();
                            }
                        }
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
                                hashMap.put("email", userEmail.toLowerCase());
                                hashMap.put("zipcode", userZipcode);

                                UIN = userEmail.replace(".", ",");
                                UIN = UIN.replace("#", "(");
                                UIN = UIN.replace("$", ")");
                                UIN = UIN.replace("/","-");
                                UIN = UIN.replace("[","+");
                                UIN = UIN.replace("]","*");
                                UIN = UIN.toLowerCase();

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


}
