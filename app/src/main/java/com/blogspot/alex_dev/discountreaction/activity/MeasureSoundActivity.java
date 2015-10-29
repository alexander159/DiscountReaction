package com.blogspot.alex_dev.discountreaction.activity;

import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.blogspot.alex_dev.discountreaction.R;
import com.blogspot.alex_dev.discountreaction.util.CameraPreview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MeasureSoundActivity extends AppCompatActivity {
    private ImageView arrowImageView;

    private MediaRecorder mRecorder;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;
    private CameraPreview mPreview;
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
        mPreview = new CameraPreview(this, mCamera);

        LinearLayout cameraPreviewLinLayout = (LinearLayout) findViewById(R.id.cameraPreviewLinLayout);

        List<Camera.Size> tmpList = mCamera.getParameters().getSupportedPreviewSizes();
        RelativeLayout.LayoutParams ll = (RelativeLayout.LayoutParams) cameraPreviewLinLayout.getLayoutParams();
        int maxWidthResolution = tmpList.get(0).width;
        int maxHeightResolution = tmpList.get(0).height;

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        //calculation of best height and shift to the left
        if (screenHeight > maxHeightResolution) {
            float scale = (float) screenHeight / (float)maxHeightResolution;   //1080 / 960 = 1.125
            ll.height = (int) (maxHeightResolution * scale);     //960 * 1.125 = 1080
            ll.width = (int) (maxWidthResolution * scale);       //1280 * 1.125 = 1440

            //shift to the left (center camera)
            int halfScreenWidth = screenWidth / 2;  //897
            int shiftPx = ll.width - halfScreenWidth;   //543
            int leftShiftPx = shiftPx / 2;  //make right and left invisible area same size  //271

            ll.leftMargin = (leftShiftPx * -1);
        }

        cameraPreviewLinLayout.setLayoutParams(ll);

        //add camera
        cameraPreviewLinLayout.addView(mPreview);

//        cameraPreviewLinLayout.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View arg0) {
//                mPreview.getmCamera().takePicture(shutterCallback, rawCallback, jpegCallback);
//            }
//        });
//
//        Toast.makeText(this, "Click me to photo", Toast.LENGTH_LONG).show();

        //startMeterTask();

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

    private void resetCam() {
        mPreview.getmCamera().startPreview();
        mPreview.setmCamera(getCameraInstance());
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            //			 Log.d(TAG, "onShutter'd");
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //			 Log.d(TAG, "onPictureTaken - raw");
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            resetCam();
            Log.d("Tag", "onPictureTaken - jpeg");
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;

            // Write to SD Card
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + "/camtest");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d("Tag", "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

                refreshGallery(outFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

    }
}
