package com.example.powergridemergencynotifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;

public class Register extends AppCompatActivity implements View.OnClickListener {

    FirebaseAuth mAuth;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        setContentView(R.layout.register);
        textView = findViewById(R.id.login_now);
        textView.setOnClickListener(this);  //listener for login button

        Button button1 = findViewById(R.id.button2);
        button1.setOnClickListener(this);  //listener for register button
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
            case R.id.button2: //register button pressed
                final EditText email_in = findViewById(R.id.email); //allow user to enter email
                final EditText password_in = findViewById(R.id.password); //allow user to enter password
                final EditText conf_password_in = findViewById(R.id.conf_password); //allow user to enter password
                String email = email_in.getText().toString().trim();  //convert email to string
                String password = password_in.getText().toString().trim();  //convert password to string
                String conf_password = conf_password_in.getText().toString();  //convert confirm password to string
                if (isNetworkOnline2()) { //check network is online
                    // Execution here
                    if (email != null && !email.isEmpty() && password != null && !password.isEmpty() && conf_password != null && !conf_password.isEmpty()) {
                        // Use the email and password as needed

                        String emailPattern = "[a-zA-Z0-9.!#$%&'*+-/=?^_`{|}~]+@[a-z]+\\.+[a-z]+";
                        // onClick of button perform this simplest code.
                        if (email.matches(emailPattern)) {
                            Toast.makeText(getApplicationContext(),"valid email address",Toast.LENGTH_SHORT).show();
                            if (password.equals(conf_password)) {
                                mAuth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    // Sign in success, update UI with the signed-in user's information
                                                    Toast.makeText(Register.this, "Account Created",
                                                            Toast.LENGTH_SHORT).show();
                                                    //store email and password for future use
                                                    SharedPreferences login = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                                    SharedPreferences.Editor editor = login.edit();
                                                    editor.putString("email", email);
                                                    editor.putString("password", password);
                                                    editor.apply();

                                                    //open next page
                                                    Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                                                    startActivity(intent1);
                                                    finish();
                                                } else {
                                                    // If sign in fails, display a message to the user.
                                                    Toast.makeText(Register.this, "Authentication failed.",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });

                            } else {
                                Toast.makeText(getApplicationContext(),"Passwords Do Not Match",Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            Toast.makeText(getApplicationContext(),"Invalid Email Address", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        // Display appropriate message when Geocoder services are not available
                        Toast.makeText(this, "Invalid login", Toast.LENGTH_LONG).show();
                    }
                    break;
                } else {
                    // Error message here if network is unavailable.
                    Toast.makeText(this, "Network is unavailable!", Toast.LENGTH_LONG).show();
                }
            case R.id.login_now: //register button pressed
                //open next page
                Intent intent1 = new Intent(this, Login.class);
                startActivity(intent1);
                finish();

            default:
                break;
        }
    }
}
