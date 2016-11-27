package com.spacedebrix.entertainment.encrgr;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    // -----------------------
    // Constants
    // -----------------------
    final int PERMISSION_RECORD_AUDIO = 1;

    // -----------------------
    // Object data
    // -----------------------
    SpeechRecognizer mySpeechRecognizer = null;
    Intent mySpeechRecognizerIntent = null;
    ValueAnimator myRecognizeFade = null;

    // -----------------------
    // Public methods
    // -----------------------
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch(requestCode) {
            case PERMISSION_RECORD_AUDIO:
                if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED )
                    initializeSpeechRecognizer();
                    startSpeechListening();
                    break;
        }
    }

    // This is just a public method to allow testing the voice recognition fade animation
    // without needed to talk
    public void startFade(View view) {
        myRecognizeFade.start();
    }

    // -----------------------
    // Protected Methods
    // -----------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MAIN", "onCreate");

        handleRecordPermissions();

        initializeRecognitionAnimation();
    }


    // -----------------------
    // Private methods
    // -----------------------
    private void initializeRecognitionAnimation() {
        if( null == myRecognizeFade ) {

            // Setup colors
            final View mainView = findViewById(R.id.activity_main);
            if( null != mainView ) {

                final int fadeFromColor = ContextCompat.getColor(getApplicationContext(), R.color.mainBackground);
                final int standardFadeToColor = ContextCompat.getColor(getApplicationContext(), R.color.standardRecognizeFadeTo);
                final int transitionDuration = getResources().getInteger(R.integer.transitionDuration);

                myRecognizeFade = ValueAnimator.ofObject(new ArgbEvaluator(), fadeFromColor, standardFadeToColor);

                myRecognizeFade.setDuration(transitionDuration);
                myRecognizeFade.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        mainView.setBackgroundColor((int) myRecognizeFade.getAnimatedValue());
                    }
                });
            }
            else {
                Log.d( "initialize", "Could not find main view" );
            }
        }
    }

    private void handleRecordPermissions() {
        // Verify/ask for permissions
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        // If we already have permissions, initialize listener and start listengin
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            initializeSpeechRecognizer();
            startSpeechListening();
        // Otherwise ask user for permissions
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_RECORD_AUDIO);
        }
    }

    private void initializeSpeechRecognizer() {
        if( null == mySpeechRecognizer ) {
            // Create speech recognizer
            mySpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

            // Create recognizer intent
            mySpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            // Intent extras, like prefer offline
            mySpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mySpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
            mySpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

            // Set listener class
            mySpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        }
    }

    private void startSpeechListening() {
        if( null != mySpeechRecognizer ) {
            mySpeechRecognizer.startListening(mySpeechRecognizerIntent);
        }
        else {
            // For some reason this got destroyed, log and try again?
            Log.d("START SPEECH LISTENING", "SpeechRecognizer object unexpected null, recreating.");
            initializeSpeechRecognizer();
            if( null != mySpeechRecognizer ) {
                mySpeechRecognizer.startListening(mySpeechRecognizerIntent);
            }
            else {
                Log.d("START SPEECH LISTENING", "SpeechRecognizer recreation failed...");
            }
        }
    }


    // -----------------------
    // Inner classes
    // -----------------------
    protected class SpeechRecognitionListener implements RecognitionListener
    {
        public SpeechRecognitionListener() {
            // Setup object data
        }

        @Override
        public void onBeginningOfSpeech()
        {
            myRecognizeFade.start();
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
        }

        @Override
        public void onError(int error)
        {
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {

        }

        @Override
        public void onPartialResults(Bundle partialResults){

        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {
        }

        @Override
        public void onResults(Bundle results)
        {
            //Log.d("OK", "onResults"); //$NON-NLS-1$
            //ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // matches are the return values of speech recognition engine
            // Use these values for whatever you wish to do
            //encourage(null);
        }

        @Override
        public void onRmsChanged(float rmsdB)
        {
        }
    }
}
