package com.example.powergridemergencynotifier;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Login extends AppCompatActivity implements View.OnClickListener{

    FirebaseAuth mAuth;
    TextView textView;
    TextView textView2;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference;
    String retZip, UIN, string_lat, string_lon;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        mAuth = FirebaseAuth.getInstance();
        textView = findViewById(R.id.register_now);
        textView.setOnClickListener(this); //listener for register button
        textView2 = findViewById(R.id.forgot_pass);
        textView2.setOnClickListener(this); //listener for forgot password button

        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference();

        Button button1 = findViewById(R.id.button2);
        button1.setOnClickListener(this);  //listener for login button
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
            case R.id.button2: //login button pressed
                final EditText email_in = findViewById(R.id.email); //allow user to enter email
                final EditText password_in = findViewById(R.id.password); //allow user to enter password
                String email = email_in.getText().toString();  //convert email to string
                String password = password_in.getText().toString();  //convert email to string
                if (isNetworkOnline2()) { //check network is online
                    // Execution here
                    if (email != null && !email.isEmpty() && password != null && !password.isEmpty()) {
                        // Use the email and password as needed
                        mAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            // Sign in success, update UI with the signed-in user's information
                                            //store email and password for future use
                                            SharedPreferences login = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                            SharedPreferences.Editor editor = login.edit();
                                            editor.putString("email", email.toLowerCase());
                                            editor.putString("password", password);
                                            editor.apply();

                                            UIN = email.replace(".", ",");
                                            UIN = UIN.replace("#", "(");
                                            UIN = UIN.replace("$", ")");
                                            UIN = UIN.replace("/","-");
                                            UIN = UIN.replace("[","+");
                                            UIN = UIN.replace("]","*");
                                            UIN = UIN.toLowerCase();

                                            //check if user has zipcode stored to database
                                            DatabaseReference userNameRef = reference.child("User").child(UIN);
                                            ValueEventListener eventListener = new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    if(!dataSnapshot.exists()) {
                                                        //no saved zipcode
                                                        Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                                                        startActivity(intent1);
                                                        finish();
                                                    }else {
                                                        //saved zipcode
                                                        //get zipcode from database
                                                        reference.child("User").child(UIN).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                if (snapshot.exists()) {
                                                                    retZip = snapshot.child("zipcode").getValue().toString();

                                                                    Toast.makeText(Login.this, retZip,
                                                                            Toast.LENGTH_SHORT).show();

                                                                    final Geocoder geocoder = new Geocoder(getApplicationContext());
                                                                    try {
                                                                        //Toast.makeText(MainActivity.this,zipcode,Toast.LENGTH_LONG).show();
                                                                        List<Address> addresses = geocoder.getFromLocationName(retZip, 5);
                                                                        //Toast.makeText(MainActivity.this,addresses.toString(),Toast.LENGTH_LONG).show();
                                                                        if (addresses != null && !addresses.isEmpty()) {
                                                                            Address address = addresses.get(0);
                                                                            // Use the address as needed
                                                                            double main_lat = address.getLatitude(); //conv to lat
                                                                            double main_lon = address.getLongitude(); //conv to long

                                                                            //convert double to string
                                                                            string_lat = String.valueOf(main_lat);
                                                                            string_lon = String.valueOf(main_lon);
                                                                        }
                                                                    }catch (IOException e) {
                                                                        throw new RuntimeException(e);
                                                                    }
                                                                    //store in shared preferences
                                                                    SharedPreferences login = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                                                    SharedPreferences.Editor editor = login.edit();
                                                                    editor.putString("zipcode", retZip);
                                                                    editor.putString("lat", string_lat);
                                                                    editor.putString("lon", string_lon);
                                                                    editor.apply();

                                                                    //open new page
                                                                    Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                                                                    startActivity(intent1);
                                                                    finish();
                                                                }
                                                            }
                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });

                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                    //error
                                                }
                                            };
                                            userNameRef.addListenerForSingleValueEvent(eventListener);


                                        } else {
                                            // If sign in fails, display a message to the user.
                                            Toast.makeText(Login.this, "Authentication failed.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });


                    } else {
                        // Display appropriate message when Geocoder services are not available
                        Toast.makeText(this, "Invalid login", Toast.LENGTH_LONG).show();
                    }
                } else {
                        // Error message here if network is unavailable.
                        Toast.makeText(this, "Network is unavailable!", Toast.LENGTH_LONG).show();
                }break;
            case R.id.register_now: //register button pressed
                if (isNetworkOnline2()) { //check network is online
                    //open next page
                    Intent intent1 = new Intent(this, Register.class);
                    startActivity(intent1);
                    finish();

                }break;
            case R.id.forgot_pass: //forgot password button pressed
                final EditText email_in2 = findViewById(R.id.email); //allow user to enter email
                String email2 = email_in2.getText().toString();  //convert email to string
                if (isNetworkOnline2() && !email2.isEmpty()) { //check network is online
                    //open next page
                    mAuth.getInstance().sendPasswordResetEmail(email2)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getApplicationContext(), "Email sent", Toast.LENGTH_LONG).show();
                                    }else {
                                        Toast.makeText(getApplicationContext(), "Invalid email", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                }if(!isNetworkOnline2()){
                    Toast.makeText(getApplicationContext(), "No internet connection", Toast.LENGTH_LONG).show();

            }if (email2.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter email", Toast.LENGTH_LONG).show();
                }

            default:
                break;
        }
    }

}
