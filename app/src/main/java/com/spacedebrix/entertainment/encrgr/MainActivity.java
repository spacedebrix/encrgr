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

import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // -----------------------
    // Constants
    // -----------------------
    final int PERMISSION_RECORD_AUDIO = 1;
    final static String ENCOURAGE_MESSAGE = "com.spacedebrix.entertainment.engrgr.ENCOURAGE";

    // -----------------------
    // Object data
    // -----------------------
    SpeechRecognizer mySpeechRecognizer = null;
    Intent mySpeechRecognizerIntent = null;
    ValueAnimator myRecognizeFade = null;
    Random myRandom = null;

    // -----------------------
    // Public methods
    // -----------------------
    @Override
    public void onStop() {
        super.onStop();

        if( null != mySpeechRecognizer ) {
            mySpeechRecognizer.destroy();
            mySpeechRecognizer = null;
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
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

    public void encourage(View v) {

        // Something has triggered the encourage, if listening, stop
        if( null != mySpeechRecognizer ) {
            mySpeechRecognizer.stopListening();
        }

        // Prepare and start Encourage activity
        Intent intent = new Intent(this, Encourage.class);

        // Get encouraging words
        intent.putExtra( ENCOURAGE_MESSAGE, getEncouragingWords() );

        startActivity(intent);
    }

    // -----------------------
    // Protected Methods
    // -----------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MAIN", "onCreate");

        myRandom = new Random();

        // TEMP DEBUG
        //runTests();

        handleRecordPermissions();

        initializeRecognitionAnimation();
    }


    // -----------------------
    // Private methods
    // -----------------------
    private String getEncouragingWords() {

        String[] templates = getEncourageTemplates();

        String template =templates[ myRandom.nextInt( templates.length ) ];

        return fillTemplate( template );
    }

    private String fillTemplate( String template ) {

        //Log.d( "REPLACING" , template );

        String value;
        String filledString = "";
        int varIndex = -1;
        int lastIndex = 0;
        int nextIndex = 0;
        while( ( varIndex = template.indexOf( '$', varIndex + 1 ) ) >= 0 ) {

            // We have start of variable, find the end
            nextIndex = getVariableEnd( template, varIndex + 1 );

            // Get the name of the variable, so we can look up the string array
            String variableName = template.substring( varIndex + 1, nextIndex );

            // Get associated string array
            int resourceId = getResources().getIdentifier( variableName.toLowerCase(), "array", this.getPackageName() );
            if( 0 < resourceId ) {
                String[] valueArray = getResources().getStringArray(resourceId);

                // Select a value
                value = valueArray[myRandom.nextInt(valueArray.length)];
                value = matchCase(variableName, value);
            }
            else {
                Log.d( "fillTemplate", "ERROR: Could not get resource ID for: " + variableName.toLowerCase() );
                value = "ERROR";
            }

            // Build
            filledString = filledString + template.substring( lastIndex, varIndex ) + value;
            lastIndex = nextIndex;

            //Log.d( "BUILDING" , filledString );
        }

        filledString = filledString + template.substring( lastIndex, template.length() );
        //Log.d( "BUILDING" , filledString );
        return filledString;
    }

    private int getVariableEnd( String template, int index ) {
        for( ; index < template.length(); ++index ) {
            if( !Character.isLetterOrDigit( template.charAt( index ) ) ) {
                return index;
            }
        }
        return index;
    }

    private String matchCase( String source, String variable ) {

        // Roughly match case.  Really there's only likely to be all lower,
        // capital first letter, or all caps.
        if( Character.isUpperCase( source.charAt( 0 ) ) ) {
            if( Character.isUpperCase( source.charAt( 1 ) ) ) {
                return variable.toUpperCase();
            }
            else {
                // Surely there's a worse way to do this.
                return variable.substring( 0, 1 ).toUpperCase() + variable.substring( 1 ).toLowerCase();
            }
        }
        else {
            return variable.toLowerCase();
        }
    }

    private String[] getEncourageTemplates() {
        return getResources().getStringArray(R.array.encourages);
    }

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

        // If we already have permissions, initialize listener and start listening
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
    protected class SpeechRecognitionListener implements RecognitionListener {

        public SpeechRecognitionListener() {
            // Setup object data
        }

        @Override
        public void onBeginningOfSpeech() {
            //Log.d("RECOGNITION_LISTENER", "onBeginningOfSpeech");
            myRecognizeFade.start();
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            //Log.d("RECOGNITION_LISTENER", "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            //Log.d("RECOGNITION_LISTENER", "onEndOfSpeech");
            mySpeechRecognizer.stopListening();
            encourage( null );
        }

        @Override
        public void onError(int error) {
            //Log.d("RECOGNITION_LISTENER", "onError");
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            //Log.d("RECOGNITION_LISTENER", "onEvent");
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            //Log.d("RECOGNITION_LISTENER", "onPartialResults");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            //Log.d("RECOGNITION_LISTENER", "onReadyForSpeech");
        }

        @Override
        public void onResults(Bundle results) {
            //Log.d("RECOGNITION_LISTENER", "onResults");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }
    }

    private void runTests() {
        fillTemplate( "Test with no variables" );
        fillTemplate( "Test with $pronoun in middle" );
        fillTemplate( "Test with $pronoun" );
        fillTemplate( "Test with $pronoun!" );
        fillTemplate( "$pronoun at beginning" );
        fillTemplate( "$pronoun, at beginning with comma" );
    }
}
