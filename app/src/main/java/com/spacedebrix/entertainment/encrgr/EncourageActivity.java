package com.spacedebrix.entertainment.encrgr;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EncourageActivity extends AppCompatActivity {

    // -----------------------
    // Public override methods
    // -----------------------
    @Override
    public void onBackPressed() {
        goBack(null);
    }

    // -----------------------
    // Public methods
    // -----------------------
    public void goBack(View view) {
        //Intent intent = new Intent(this, MainActivity.class);

        //startActivity(intent);
        finish();
    }

    // -----------------------
    // Protected override methods
    // -----------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encourage);

        // Initialize UI bullshit
        initUi();

        // Get message from intent and set it to screen
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.ENCOURAGE_MESSAGE);
        setEncourage( message );
    }

    // -----------------------
    // Private methods
    // -----------------------
    private void initUi() {

        overridePendingTransition(0,0);
        setFadeAnimation((ViewGroup) findViewById(android.R.id.content));
    }

    private void setEncourage( String message ) {

        TextView textView = (TextView) findViewById(R.id.textView);

        textView.setText(message);
    }

    private void setFadeAnimation(ViewGroup panel) {

        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(getResources().getInteger(R.integer.encourageFlashDuration));
        set.addAnimation(animation);

        LayoutAnimationController controller =
                new LayoutAnimationController(set);
        panel.setLayoutAnimation(controller);
    }
}
