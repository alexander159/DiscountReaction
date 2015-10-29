package com.blogspot.alex_dev.discountreaction.activity;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.blogspot.alex_dev.discountreaction.R;
import com.blogspot.alex_dev.discountreaction.util.CameraPreview;

import java.util.concurrent.TimeUnit;

public class MeasureSoundActivity extends AppCompatActivity {
    private ImageView arrowImageView;

    private MediaRecorder mRecorder;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;

    private boolean isMeasuring;
    private MeterArrowTask mTask;

    private float lastDegree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_sound);

        arrowImageView = (ImageView) findViewById(R.id.arrowImageView);

        lastDegree = 0f;
        moveMeterArrow(lastDegree);

        Camera mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        CameraPreview mPreview = new CameraPreview(this, mCamera);

        LinearLayout cameraPreviewLinLayout = (LinearLayout) findViewById(R.id.cameraPreviewLinLayout);

//        List<Camera.Size> tmpList = mCamera.getParameters().getSupportedPreviewSizes();
//        ScrollView.LayoutParams ll = (ScrollView.LayoutParams) cameraPreviewLinLayout.getLayoutParams();
//        ll.width = tmpList.get(0).width;
//        ll.height = tmpList.get(0).height;
//
//        DisplayMetrics metrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        int width = metrics.widthPixels;
//        int height = metrics.heightPixels;
//
//        if (ll.height < screenHeight) {
//            float scale = (float) screenHeight / (float) ll.height;
//            ll.height *= scale;
//            ll.width *= scale;
//        }
//        cameraPreviewLinLayout.setLayoutParams(ll);

        //add camera
        cameraPreviewLinLayout.addView(mPreview);

        startMeterTask();

//        try {
//            mPreview.startRecording();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    public void onResume() {
        super.onResume();
        startRecorder();
    }

    public void onPause() {
        super.onPause();
        stopRecorder();
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = android.hardware.Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();// Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
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
                android.util.Log.e("[Monkey]", "IOException: " + android.util.Log.getStackTraceString(ioe));

            } catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " + android.util.Log.getStackTraceString(e));
            }
            try {
                mRecorder.start();
            } catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " + android.util.Log.getStackTraceString(e));
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
}
