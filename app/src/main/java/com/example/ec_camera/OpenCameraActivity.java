package com.example.ec_camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class OpenCameraActivity extends AppCompatActivity {

    private static OpenCameraActivity openCameraActivity;
    FrameLayout frameLayout;
    Button btn_login_pin;
    Button btn_confrim;
    public static OpenCameraActivity getMainActivity() {
        return openCameraActivity;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_camera);
        frameLayout = findViewById(R.id.frameLayout);
        frameLayout.setClickable(true);
        if(frameLayout.isClickable()){
            loadFragment(new EmployeeScanFragment());

        }
        btn_login_pin = findViewById(R.id.login_pin);
        btn_login_pin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), HomeViewActivity.class);
                view.getContext().startActivity(intent);
            }
        });

    }
    private void loadFragment(Fragment fragment) {
        // create a FragmentManager
        FragmentManager fm = getSupportFragmentManager();
        // create a FragmentTransaction to begin the transaction and replace the Fragment
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        // replace the FrameLayout with new Fragment
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit(); // save the changes
    }
}