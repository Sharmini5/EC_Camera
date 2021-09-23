package com.example.ec_camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.widget.Toolbar;

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

public class ConfigureSettingActivity extends AppCompatActivity {
    TabLayout tabLayout;
    ViewPager viewPager;
    PageAdapter pageAdapter;
    TabItem tabChats;
    TabItem tabStatus;
    TabItem tabCalls;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_setting);


        tabLayout = findViewById(R.id.tablayout);
        tabChats = findViewById(R.id.tabChats);
        tabStatus = findViewById(R.id.tabStatus);
        tabCalls = findViewById(R.id.tabCalls);
        viewPager = findViewById(R.id.viewPager);

        pageAdapter = new PageAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(pageAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                if (tab.getPosition() == 1) {
                    tabLayout.setBackgroundColor(ContextCompat.getColor(ConfigureSettingActivity.this,
                            R.color.labelColor));

                } else if (tab.getPosition() == 2) {

                    tabLayout.setBackgroundColor(ContextCompat.getColor(ConfigureSettingActivity.this,
                            android.R.color.darker_gray));

                } else {

                    tabLayout.setBackgroundColor(ContextCompat.getColor(ConfigureSettingActivity.this,
                            R.color.black));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }
}