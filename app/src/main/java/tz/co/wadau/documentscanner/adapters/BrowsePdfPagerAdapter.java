package tz.co.wadau.documentscanner.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import tz.co.wadau.documentscanner.fragments.DevicePdfFragment;
import tz.co.wadau.documentscanner.fragments.RecentPdfFragment;


public class BrowsePdfPagerAdapter extends FragmentPagerAdapter {

    public BrowsePdfPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        Fragment fragment = null;

        switch (position) {
            case 0:
                fragment = new RecentPdfFragment();
                break;
            case 1:
                fragment = new DevicePdfFragment();
                break;
        }

        return fragment;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
