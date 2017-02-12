package com.spacedebrix.entertainment.encrgr;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by landon on 2/11/17.
 */

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
