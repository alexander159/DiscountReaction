package com.blogspot.alex_dev.discountreaction.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blogspot.alex_dev.discountreaction.R;
import com.blogspot.alex_dev.discountreaction.util.CameraPreview;
import com.blogspot.alex_dev.discountreaction.util.Constants;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MeasureReactionActivity extends AppCompatActivity {
    static final private double EMA_FILTER = 0.6;
    private static final String TAG = "MeasureReactionActivity";
    private static double mEMA = 0.0;
    private ImageView arrowImageView;
    private TextView timeCounterTextView;
    private CameraPreview preview;
    private boolean isDbMeasuring;
    private float lastDegree;
    private boolean isDbLevelReached;
    private int timeLeft;

    private int dbTopValue;     //decibels maximum value to win

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_reaction);

        arrowImageView = (ImageView) findViewById(R.id.arrowImageView);
        timeLeft = 5;
        timeCounterTextView = (TextView) findViewById(R.id.timeCounterTextView);
        timeCounterTextView.setText(String.valueOf(timeLeft));

        lastDegree = 0f;
        isDbLevelReached = false;
        moveMeterArrow(lastDegree);

        SharedPreferences sPref = getSharedPreferences(Constants.SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
        dbTopValue = sPref.getInt(Constants.SHARED_PREF_DB_VALUE, 80);

        // Create our Preview view and set it as the content of our activity.
        preview = new CameraPreview(this);

        LinearLayout cameraPreviewLinLayout = (LinearLayout) findViewById(R.id.cameraPreviewLinLayout);

        List<Camera.Size> tmpList = preview.getCamera().getParameters().getSupportedPreviewSizes();
        RelativeLayout.LayoutParams cameraLP = (RelativeLayout.LayoutParams) cameraPreviewLinLayout.getLayoutParams();
        int maxWidthResolution = tmpList.get(0).width;
        int maxHeightResolution = tmpList.get(0).height;

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        //calculation of best height and shift to the left
        float scale = (float) screenHeight / (float) maxHeightResolution;
        cameraLP.height = (int) (maxHeightResolution * scale);
        cameraLP.width = (int) (maxWidthResolution * scale);

        //shift to the left (center camera)
        int halfScreenWidth = screenWidth / 2;
        int shiftPx = cameraLP.width - halfScreenWidth;
        int leftShiftPx = shiftPx / 2;  //make right and left invisible area equal size

        cameraLP.leftMargin = (leftShiftPx * -1);

        cameraPreviewLinLayout.setLayoutParams(cameraLP);
        cameraPreviewLinLayout.addView(preview); //add camera

        //center counter
        RelativeLayout.LayoutParams counterLP = (RelativeLayout.LayoutParams) timeCounterTextView.getLayoutParams();
        counterLP.topMargin = (screenHeight / 10) - (counterLP.height / 2);
        counterLP.leftMargin = (screenWidth / 4) - (counterLP.width / 2);
    }

    @Override
    protected void onStart() {
        super.onStart();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                disableAllAudio();

                preview.startRecording();
                isDbMeasuring = true;
                new TimerCountdown().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                new DbMeasuringTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }, 1000); //test
    }

    private void disableAllAudio() {
        // disable sound when recording.
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_ALARM, true);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_DTMF, true);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_MUSIC, true);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_RING, true);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_SYSTEM, true);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_VOICE_CALL, true);
    }

    private void enableAllAudio() {
        // re-enable sound after recording.
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_ALARM, false);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_DTMF, false);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_MUSIC, false);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_RING, false);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_SYSTEM, false);
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_VOICE_CALL, false);
    }

    private double dbToDegree(double dbVal) {
        //input val in the range -90 .. 0 (0 is the highest value)
        //2 degree per each value
        return (dbVal + 90) * 2;
    }

    private void moveMeterArrow(float degree) {
        RotateAnimation animRotate = new RotateAnimation(lastDegree, degree, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        animRotate.setDuration(0);
        animRotate.setFillEnabled(true);
        animRotate.setFillAfter(true);

        arrowImageView.startAnimation(animRotate);

        lastDegree = degree;
    }

//    public void startRecorder() {
//        if (mRecorder == null) {
//            mRecorder = new MediaRecorder();
//            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            mRecorder.setOutputFile("/dev/null");
//            try {
//                mRecorder.prepare();
//            } catch (java.io.IOException ioe) {
//                Log.e(TAG, "IOException: " + android.util.Log.getStackTraceString(ioe));
//
//            } catch (java.lang.SecurityException e) {
//                Log.e(TAG, "SecurityException: " + android.util.Log.getStackTraceString(e));
//            }
//            try {
//                mRecorder.start();
//            } catch (java.lang.SecurityException e) {
//                Log.e(TAG, "SecurityException: " + android.util.Log.getStackTraceString(e));
//            }
//
//            //mEMA = 0.0;
//        }
//
//    }
//
//    public void stopRecorder() {
//        if (mRecorder != null) {
//            mRecorder.stop();
//            mRecorder.release();
//            mRecorder = null;
//        }
//    }

    public double soundDb(double ampl) {
        //return  20 * Math.log10(getAmplitudeEMA() / ampl);
        return 20 * Math.log10(getAmplitude() / ampl);
    }

    public double getAmplitude() {
        if (preview.getMediaRecorder() != null)
            return (preview.getMediaRecorder().getMaxAmplitude());
        else
            return 0;
    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

    public void setIsDbLevelReached(boolean isDbLevelReached) {
        this.isDbLevelReached = isDbLevelReached;
    }

    private boolean isDbLevelReached() {
        return isDbLevelReached;
    }

    class DbMeasuringTask extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            while (isDbMeasuring) {
                publishProgress((int) (dbToDegree(soundDb(65535.0))));
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            System.out.println("Value:" + values[0]);
            moveMeterArrow(values[0]);

            isDbLevelReached = ((values[0] / 2) > dbTopValue);

            if (isDbLevelReached()) {
                isDbMeasuring = false;
                preview.stopRecording();
                //enableAllAudio();

                Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                intent.putExtra(ResultActivity.RESULT_ID, ResultActivity.SUCCESS);
                startActivity(intent);
                finish();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    class TimerCountdown extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            while (timeLeft >= 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                    publishProgress(timeLeft);
                    --timeLeft;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            timeCounterTextView.setText(String.valueOf(values[0]));
        }

        @Override
        protected void onPostExecute(Integer value) {
            super.onPostExecute(value);

            //level success wasn't reached
            if (!isDbLevelReached()) {
                isDbMeasuring = false;
                preview.stopRecording();
                //enableAllAudio();

                Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                intent.putExtra(ResultActivity.RESULT_ID, ResultActivity.FAILURE);
                startActivity(intent);
                finish();
            }
        }
    }
}
