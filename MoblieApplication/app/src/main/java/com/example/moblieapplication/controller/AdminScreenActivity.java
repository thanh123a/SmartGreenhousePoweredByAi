package com.example.moblieapplication.controller;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.moblieapplication.R;
import com.example.moblieapplication.adapter.AdminViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminScreenActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_screen);

        bottomNavigationView = findViewById(R.id.bottom_nav_admin);
        viewPager = findViewById(R.id.view_pager_admin);

        setViewPager();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.admin_action_home) {
                viewPager.setCurrentItem(0);
            }
            if (item.getItemId() == R.id.admin_action_option) {
                viewPager.setCurrentItem(1);
            }

            return true;
        });

    }

    private void setViewPager() {
        AdminViewPagerAdapter viewPagerAdapter = new AdminViewPagerAdapter(getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(viewPagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNavigationView.getMenu().findItem(R.id.admin_action_home).setChecked(true);
                        break;
                    case 1:
                        bottomNavigationView.getMenu().findItem(R.id.admin_action_option).setChecked(true);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

}