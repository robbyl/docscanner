package tz.co.wadau.documentscanner.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import tz.co.wadau.documentscanner.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static final String KEY_PREFS_STAY_AWAKE = "prefs_stay_awake";
    public static final String KEY_PREFS_REMEMBER_LAST_PAGE = "prefs_remember_last_page";
    private Context context;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        context = getContext();
//        bindPreferenceSummaryToValue(findPreference(KEY_PREFS_STAY_AWAKE));
//        bindPreferenceSummaryToValue(findPreference(KEY_PREFS_REMEMBER_LAST_PAGE));
    }

    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

//            switch (key) {
//                case KEY_PREFS_STAY_AWAKE:
//                    bindPreferenceSummaryToValue(findPreference(KEY_PREFS_STAY_AWAKE));
//                    break;
//                case KEY_PREFS_REMEMBER_LAST_PAGE:
//                    bindPreferenceSummaryToValue(findPreference(KEY_PREFS_REMEMBER_LAST_PAGE));
//                    break;
//            }
        }
    };


    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        preference.setSummary(sharedPreferences.getString(preference.getKey(), ""));
    }
}

