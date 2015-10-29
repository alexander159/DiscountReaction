package com.blogspot.alex_dev.discountreaction.activity;

import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blogspot.alex_dev.discountreaction.R;
import com.blogspot.alex_dev.discountreaction.util.CameraPreview;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MeasureReactionActivity extends AppCompatActivity {
    static final private double EMA_FILTER = 0.6;
    private static final String TAG = "MeasureReactionActivity";
    private static double mEMA = 0.0;
    private ImageView arrowImageView;
    private TextView timeCounterTextView;
    private MediaRecorder mRecorder;
    private CameraPreview preview;
    private boolean isMeasuring;
    private MeterArrowTask mTask;
    private float lastDegree;
    private boolean isDbLevelReached;
    private int timeLeft;

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
        counterLP.topMargin = (screenHeight / 10) - (counterLP.height/2);
        counterLP.leftMargin = (screenWidth / 4) - (counterLP.width / 2);

        new TimerCountdown().execute();
        //startMeterTask();
    }

//
//    public void onResume() {
//        super.onResume();
//        startRecorder();
//    }
//
//    public void onPause() {
//        super.onPause();
//        stopRecorder();
//    }

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


    public void startMeterTask() {
        startRecorder();
        isMeasuring = true;
        mTask = new MeterArrowTask();
        mTask.execute();
    }

    public void stopMeterTask() {
        stopRecorder();
        isMeasuring = false;
        mTask = null;
    }

    public void startRecorder() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
            } catch (java.io.IOException ioe) {
                Log.e(TAG, "IOException: " + android.util.Log.getStackTraceString(ioe));

            } catch (java.lang.SecurityException e) {
                Log.e(TAG, "SecurityException: " + android.util.Log.getStackTraceString(e));
            }
            try {
                mRecorder.start();
            } catch (java.lang.SecurityException e) {
                Log.e(TAG, "SecurityException: " + android.util.Log.getStackTraceString(e));
            }

            //mEMA = 0.0;
        }

    }

    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double soundDb(double ampl) {
        return 20 * Math.log10(getAmplitude() / ampl);
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return (mRecorder.getMaxAmplitude());
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

    class MeterArrowTask extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            while (isMeasuring) {
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
            if (isDbLevelReached()) {
                Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                intent.putExtra(ResultActivity.RESULT_ID, ResultActivity.SUCCESS);
                startActivity(intent);
                finish();
            }
        }

        @Override
        protected void onPostExecute(Integer value) {
            super.onPostExecute(value);

            //level success wasn't reached
            if (!isDbLevelReached()) {
                Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                intent.putExtra(ResultActivity.RESULT_ID, ResultActivity.FAILURE);
                startActivity(intent);
                finish();
            }
        }
    }
}
