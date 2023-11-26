package com.example.powergridemergencynotifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class NotificationPage extends AppCompatActivity {
    DatabaseReference alertRef;
    DatabaseReference firebaseDatabase;
    TableLayout tl;
    TableRow tr_head;
    ArrayList<String> checkMessage = new ArrayList<>();
    TextView tv1;
    TextView tv2;
    TextView tv3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout file as the content view.
        setContentView(R.layout.notif);

        tl = (TableLayout) findViewById(R.id.tablelayout);
        tl.removeViews(1,Math.max(0, tl.getChildCount() - 1));

        //show action bar for back button visibility
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (isNetworkOnline2()) { //check network is online

            SharedPreferences notif_page = PreferenceManager.getDefaultSharedPreferences(this);
            //retrieve stored data to display on page
            String UID = notif_page.getString("UID", "");
            //use UID to get tweets
            firebaseDatabase = FirebaseDatabase.getInstance().getReference();
            alertRef = firebaseDatabase.child("Alerts").child(UID);
            //Toast.makeText(NotificationPage.this, UID, Toast.LENGTH_LONG).show();
            //making var for text views on page
            tv1 = findViewById(R.id.textView1);
            tv2 = findViewById(R.id.textView2);
            tv3 = findViewById(R.id.textView3);


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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //back button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    private void PopulateTable(){
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot dsp : dataSnapshot.getChildren()){
                    String label = dsp.getKey().toString(); //gets UID
                    //Toast.makeText(NotificationPage.this, label,Toast.LENGTH_LONG).show();
                    if (label.equals("Date")){
                        //do nothing
                    } else if (label.equals("Category")) {
                        String categoryLabel = dsp.getValue(String.class);
                        tv1.setText(categoryLabel);
                    } else if (label.equals("City")) {
                        String cityLabel = dsp.getValue(String.class);
                        tv3.setText(cityLabel);
                    } else if (label.equals("Type")) {
                        String typeLabel = dsp.getValue(String.class);
                        tv2.setText(typeLabel);
                    } else{
                        //Toast.makeText(NotificationPage.this, label,Toast.LENGTH_LONG).show();
                        String message = dsp.getValue(String.class); //gets message from database

                        String time = message.substring(0, 5); //first four of message which is time
                        String newMessage = message.substring(5, message.length()); //remaining of message
                        //Toast.makeText(NotificationPage.this, time, Toast.LENGTH_LONG).show();
                        //Toast.makeText(NotificationPage.this, newMessage, Toast.LENGTH_LONG).show();
                        tr_head = new TableRow(getApplicationContext());
                        tl.addView(tr_head, new TableLayout.LayoutParams());

                            TextView labelTime = new TextView(NotificationPage.this);
                            labelTime.setText(time);
                            labelTime.setWidth(0);
                            labelTime.setGravity(Gravity.CENTER);
                            labelTime.setTextSize(14);
                            labelTime.setPadding(18, 18, 18, 18);
                            tr_head.addView(labelTime);// add the column to the table row here

                            TextView labelMessage = new TextView(NotificationPage.this);    // part3
                            labelMessage.setText(newMessage); // set the text for the header
                            labelMessage.setGravity(Gravity.CENTER);
                            labelMessage.setWidth(0);
                            labelMessage.setTextSize(14);
                            labelMessage.setPadding(5, 5, 5, 5); // set the padding (if required)
                            tr_head.addView(labelMessage); // add the column to the table row here
                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationPage.this, "cancelled", Toast.LENGTH_LONG).show();
            }
        };
        alertRef.addListenerForSingleValueEvent(eventListener);
    }
}
