package tz.co.wadau.documentscanner.utils;

import android.app.Activity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import static tz.co.wadau.documentscanner.PDFToolsActivity.ADMOB_APP_ID;


public class AdManager {
    // Static fields are shared between all instances.
    private static InterstitialAd interstitialAd;

    private Activity activity;
    private String AD_UNIT_ID;

    public AdManager(Activity activity, String AD_UNIT_ID) {

        this.activity = activity;
        this.AD_UNIT_ID = AD_UNIT_ID;
    }

    public void createAd() {
        // Create an ad.
        //Initialize adMob Ads
        MobileAds.initialize(activity, ADMOB_APP_ID);

        interstitialAd = new InterstitialAd(activity);
        interstitialAd.setAdUnitId(AD_UNIT_ID);

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("50941AF57FD6434ECCFA81A57FF7D313")
                .addTestDevice("28D01E8B9AD20EEC0A73479ED41140E9")
                .addTestDevice("416D95A4A025645F8068A752DABC068F")
                .build();

        // Load the interstitial ad.
        interstitialAd.loadAd(adRequest);
    }

    public static InterstitialAd getAd() {
        if (interstitialAd != null && interstitialAd.isLoaded()) {
            return interstitialAd;
        } else return null;
    }
}