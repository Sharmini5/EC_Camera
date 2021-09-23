package com.example.ec_camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomeViewActivity extends AppCompatActivity {
Button btn_confrim;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        btn_confrim = findViewById(R.id.retake);
        btn_confrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), PhotoLoadingActivity.class);
                view.getContext().startActivity(intent);
            }
        });
    }
}