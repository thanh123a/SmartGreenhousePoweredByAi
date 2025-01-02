package com.example.moblieapplication.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.moblieapplication.fragment.AiChatBotFragment;
import com.example.moblieapplication.fragment.HomeFragment;
import com.example.moblieapplication.fragment.OptionFragment;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {
    public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new AiChatBotFragment();
            case 2:
                return new OptionFragment();
            default:
                return new HomeFragment();
        }

    }

    @Override
    public int getCount() {
        return 3;
    }
}
