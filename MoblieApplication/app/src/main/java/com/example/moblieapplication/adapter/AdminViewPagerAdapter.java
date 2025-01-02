package com.example.moblieapplication.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.moblieapplication.fragment.AdminHomeFragment;
import com.example.moblieapplication.fragment.OptionFragment;

public class AdminViewPagerAdapter extends FragmentStatePagerAdapter {
    public AdminViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new AdminHomeFragment();
            case 1:
                return new OptionFragment();
            default:
                return new AdminHomeFragment();
        }
    }

    @Override
    public int getCount() {
        return 2;
    }
}
