package com.example.linda.giffychat.Main;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class RoomTabAdapter extends FragmentPagerAdapter {

    public RoomTabAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return RoomTabFragment.newInstance(position + 1);
    }

    @Override
    public int getCount() {
        return 2;
    }

}
