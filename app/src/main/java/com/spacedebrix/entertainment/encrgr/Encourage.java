package com.spacedebrix.entertainment.encrgr;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class Encourage extends AppCompatActivity {

    // -----------------------
    // Public methods
    // -----------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encourage);

        TextView text = (TextView) findViewById(R.id.textView);
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.ENCOURAGE_MESSAGE);

        text.setText(message);
    }

    @Override
    public void onBackPressed() {
        goBack(null);
    }

    public void goBack(View view) {
        Intent intent = new Intent(this, MainActivity.class);

        startActivity(intent);
        finish();
    }

}
