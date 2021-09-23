package com.example.ec_camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class PlaceFaceActivity extends AppCompatActivity {
    private static int TIME_OUT = 5000; //Time to launch the another activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_face);

        FrameLayout btnstart = findViewById(R.id.frameLayout);
        //Declare Text fields to show time left
        final TextView mCounter=(TextView)findViewById(R.id.timer);
        TextView textPlace=(TextView)findViewById(R.id.placeFace);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                textPlace.setVisibility(View.GONE);
                mCounter.setVisibility(View.VISIBLE);
                mCounter.setText("3");
            }
        }, 1000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                textPlace.setVisibility(View.GONE);
                mCounter.setVisibility(View.VISIBLE);
                mCounter.setText("2");
            }
        }, 2000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                textPlace.setVisibility(View.GONE);
                mCounter.setVisibility(View.VISIBLE);
                mCounter.setText("1");
            }
        }, 3000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                textPlace.setVisibility(View.GONE);
                mCounter.setVisibility(View.VISIBLE);
                mCounter.setText("1");
            }
        }, 4000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(PlaceFaceActivity.this, OpenCameraActivity.class);
                startActivity(i);
            }
        }, TIME_OUT);
    }
}