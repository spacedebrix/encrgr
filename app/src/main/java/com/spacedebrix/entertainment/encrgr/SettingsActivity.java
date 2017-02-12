package com.spacedebrix.entertainment.encrgr;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    // -----------------------
    // Public override methods
    // -----------------------
    @Override
    public void onBackPressed() {
        // Main was destroyed, recreate it
        if( getResources().getBoolean(R.bool.finishMainOnEncourage) ) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

        finish();
    }

    // -----------------------
    // Protected override methods
    // -----------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
