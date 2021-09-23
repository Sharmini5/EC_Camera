package com.example.ec_camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class Scan_Face extends AppCompatActivity {
Button btn_login_pin;
    Button btn_config;
FrameLayout face_frame;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_face);
        btn_login_pin = findViewById(R.id.login_pin);
        btn_login_pin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), LoginWithPinActivity.class);
                view.getContext().startActivity(intent);
            }
        });
        btn_config = findViewById(R.id.config);
        btn_config.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ConfigureSettingActivity.class);
                view.getContext().startActivity(intent);
            }
        });
        face_frame = findViewById(R.id.frameLayout);
        face_frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), PlaceFaceActivity.class);
                view.getContext().startActivity(intent);
            }
        });
    }
}