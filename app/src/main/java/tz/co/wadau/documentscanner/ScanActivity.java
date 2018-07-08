package tz.co.wadau.documentscanner;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;

import java.util.Locale;

public class ScanActivity extends Activity {

    public static final String EXTRA_BRAND_IMG_RES = "title_img_res";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_LANGUAGE = "language";
    public static final String EXTRA_ACTION_BAR_COLOR = "ab_color";
    public static final String RESULT_IMAGE_PATH = ScanFragment.RESULT_IMAGE_PATH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        int titleImgRes = getIntent().getExtras().getInt(EXTRA_BRAND_IMG_RES);
        int abColor = getIntent().getExtras().getInt(EXTRA_ACTION_BAR_COLOR);
        String title = getIntent().getExtras().getString(EXTRA_TITLE);
        String locale = getIntent().getExtras().getString(EXTRA_LANGUAGE);

        if (locale != null) {
            Locale l = new Locale(locale);
            Locale.setDefault(l);
            Configuration config = new Configuration();
            config.locale = l;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }

        if (title != null) setTitle(title);
//        if (titleImgRes != 0) getActionBar().setLogo(titleImgRes);

        if (abColor != 0) {
//            getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(abColor)));
//            getActionBar().setDisplayShowTitleEnabled(false);
//            getActionBar().setDisplayShowTitleEnabled(true);
        }

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            if (getIntent().getExtras() != null) {
                args.putAll(getIntent().getExtras());
            }

            FragmentManager fragMan = getFragmentManager();
            Fragment f = new ScanFragment();
            f.setArguments(args);
            FragmentTransaction fragTransaction = fragMan.beginTransaction();
            fragTransaction.replace(R.id.contaner, f, "scan_frag").commit();
        }
    }

    @Override
    public void onBackPressed() {
        ScanFragment scanFragment = (ScanFragment) getFragmentManager().findFragmentByTag("scan_frag");
        if (scanFragment != null) {
            boolean exit = scanFragment.onBackPressed();
            if (exit) {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

}