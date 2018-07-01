package tz.co.wadau.documentscanner.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import tz.co.wadau.documentscanner.fragments.BookmarksFragment;
import tz.co.wadau.documentscanner.fragments.TableContentsFragment;

public class ContentsPagerAdapter extends FragmentPagerAdapter {
    private String pdfPath;

    public ContentsPagerAdapter(FragmentManager fm, String pdfPath) {
        super(fm);
        this.pdfPath = pdfPath;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = TableContentsFragment.newInstance(pdfPath);
                break;
            case 1:
                fragment = BookmarksFragment.newInstance(pdfPath);
                break;
        }

        return fragment;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
