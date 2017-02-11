package com.spacedebrix.entertainment.encrgr;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
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
    boolean myHasListeningPermissions = false;
    List<TextView> myTextViews = null;
    Thread mySuggestionThread = null;

    // -----------------------
    // Public methods
    // -----------------------
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch(requestCode) {
            case PERMISSION_RECORD_AUDIO:
                if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED )
                    myHasListeningPermissions = true;
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

        // Prepare and start EncourageActivity activity
        Intent intent = new Intent(this, EncourageActivity.class);

        // Get encouraging words
        intent.putExtra( ENCOURAGE_MESSAGE, getEncouragingWords() );

        startActivity(intent);

        // Finish this activity when we leave
        if( getResources().getBoolean(R.bool.finishMainOnEncourage)) {
            finish();
        }
    }

    // -----------------------
    // Protected Methods
    // -----------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Log.d("MAIN", "onCreate");

        myRandom = new Random();

        // TEMP DEBUG
        //runTests();

        handleRecordPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        overridePendingTransition(0,0);
        initializeRecognitionAnimation();
        startSpeechListening();
        startSuggestions();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if( null != mySpeechRecognizer ) {
            mySpeechRecognizer.stopListening();
        }
        stopSuggestions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( null != mySpeechRecognizer ) {
            mySpeechRecognizer.destroy();
            mySpeechRecognizer = null;
        }
        finish();
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

    private String[] getEncourageSuggestions() {
        return getResources().getStringArray(R.array.suggestions);
    }

    private void initializeRecognitionAnimation() {

        // Get colors
        final int fadeFromColor = ContextCompat.getColor(getApplicationContext(), R.color.mainBackground);
        final int standardFadeToColor = ContextCompat.getColor(getApplicationContext(), R.color.standardRecognizeFadeTo);
        final int transitionDuration = getResources().getInteger(R.integer.transitionDuration);

        // Make sure we can find our view
        final View mainView = findViewById(R.id.activity_main);
        if( null != mainView ) {

            // Start with background correct color
            mainView.setBackgroundColor(fadeFromColor);

            // If we haven't already created this, create it
            if( null == myRecognizeFade ) {

                // Setup colors
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
            myHasListeningPermissions = true;
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
        if( myHasListeningPermissions ) {
            if (null != mySpeechRecognizer) {
                mySpeechRecognizer.stopListening();
                mySpeechRecognizer.startListening(mySpeechRecognizerIntent);
            } else {
                // For some reason this got destroyed, log and try again?
                Log.d("START SPEECH LISTENING", "SpeechRecognizer object unexpectedly null, recreating.");
                initializeSpeechRecognizer();
                if (null != mySpeechRecognizer) {
                    mySpeechRecognizer.startListening(mySpeechRecognizerIntent);
                } else {
                    Log.d("START SPEECH LISTENING", "SpeechRecognizer recreation failed...");
                }
            }
        }
    }

    private void startSuggestions() {

        final String[] suggestions = getEncourageSuggestions();
        final RelativeLayout thisLayout = (RelativeLayout)findViewById( R.id.activity_main );

        mySuggestionThread = new Thread(){
            public void run() {
            while (true) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        TextView tv = getSuggestionTextView(suggestions[myRandom.nextInt(suggestions.length)]);
                        thisLayout.addView(tv, 0);
                        setSuggestionAnimation(tv);
                    }
                });
                try {
                    Thread.sleep(getResources().getInteger(R.integer.suggestionSpawnRate));
                } catch (InterruptedException e) {
                    return;
                }
            }
        }};

        mySuggestionThread.start();
    }

    private void stopSuggestions() {
        if( null != mySuggestionThread ) {
            mySuggestionThread.interrupt();
        }
    }

    private TextView getSuggestionTextView( String suggestion ) {

        TextView tv = new TextView( MainActivity.this );
        tv.setText( suggestion );
        tv.setTextSize( getResources().getInteger(R.integer.suggestionSize) );
        tv.setTextColor( ContextCompat.getColor(getApplicationContext(), R.color.suggestions) );
        tv.setShadowLayer(getResources().getInteger(R.integer.suggestionShadowRadius),
                0,
                0,
                ContextCompat.getColor(getApplicationContext(), R.color.suggestions));
        tv.setGravity(Gravity.CENTER);

        return tv;

    }

    private void setSuggestionAnimation( final TextView tv ) {

        AnimationSet set = new AnimationSet(true);

        TypedValue typedValue = new TypedValue();
        getResources().getValue( R.dimen.suggestionMaxAlpha, typedValue, true );
        float maxAlpha = typedValue.getFloat();

        int fadeInDuration = getResources().getInteger(R.integer.suggestionFadeInDuration);
        int fadeStayDuration = getResources().getInteger(R.integer.suggestionStayDuration);
        int fadeOutDuration = getResources().getInteger(R.integer.suggestionFadeOutDuration);
        int fadeTotalDuration = fadeInDuration + fadeStayDuration + fadeOutDuration;

        Animation animation = new AlphaAnimation(0.0f, maxAlpha);
        animation.setDuration(fadeInDuration);
        set.addAnimation(animation);

        animation = new AlphaAnimation(maxAlpha, 0.0f);
        animation.setDuration(fadeOutDuration);
        animation.setStartOffset( fadeInDuration + fadeStayDuration);
        set.addAnimation(animation);

        Point startPoint = new Point();
        Point endPoint = new Point();
        setAnimationTranslatePoints(startPoint, endPoint);

        final RelativeLayout thisLayout = (RelativeLayout)findViewById( R.id.activity_main );
        animation = new TranslateAnimation( startPoint.x, endPoint.x, startPoint.y, endPoint.y );
        animation.setDuration(fadeTotalDuration);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                // without the post method, the main UI crashes if the view is removed
                thisLayout.post(new Runnable() {
                    public void run() {
                        // it works without the runOnUiThread, but all UI updates must
                        // be done on the UI thread
                        runOnUiThread(new Runnable() {
                            public void run() {
                                thisLayout.removeView(tv);
                            }
                        });
                    }
                });
            }
        });
        set.addAnimation(animation);

        tv.startAnimation(set);
    }

    private void setAnimationTranslatePoints(Point startPoint, Point endPoint) {

        TypedValue typedValue = new TypedValue();
        getResources().getValue( R.dimen.suggestionStartBufferX, typedValue, true );
        float startBufferX = typedValue.getFloat();

        getResources().getValue( R.dimen.suggestionStartOffsetX, typedValue, true );
        float startOffsetX = typedValue.getFloat();

        getResources().getValue( R.dimen.suggestionStartBufferY, typedValue, true );
        float startBufferY = typedValue.getFloat();

        getResources().getValue( R.dimen.suggestionStartOffsetY, typedValue, true );
        float startOffsetY = typedValue.getFloat();

        Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        startPoint.x = (int)( (startOffsetX * screenSize.x) + (startBufferX * myRandom.nextInt(screenSize.x)));
        startPoint.y = (int)( (startOffsetY * screenSize.y) + (startBufferY * myRandom.nextInt(screenSize.y)));

        endPoint.x = startPoint.x  - getResources().getInteger(R.integer.suggestionTravelX);
        endPoint.y = startPoint.y;

        Log.d("SPAWN SUGGESTION", "Start = " + startPoint.x + ", " + startPoint.y + " End = " + endPoint.x + ", " + endPoint.y );
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
        String[] templates = getEncourageTemplates();

        for (String s: templates) {
            Log.d( "TEST", fillTemplate(s) );
        }

    }
}
