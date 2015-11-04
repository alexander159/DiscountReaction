package com.blogspot.alex_dev.discountreaction.activity;

import android.content.Context;
import android.content.Intent;
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
    private static final String TAG = "MeasureReactionActivity";
    private ImageView arrowImageView;
    private TextView timeCounterTextView;
    private CameraPreview preview;
    private boolean isDbMeasuring;
    private float lastMeterDegree;
    private boolean isDbLevelReached;
    private boolean isApplicationInterrupted;
    private int dbTopValue;     //decibel maximum value to win
    private int secondsLeft;
    private int[] soundLevelValue;  //save sound level of all sources before muting

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_reaction);

        arrowImageView = (ImageView) findViewById(R.id.arrowImageView);
        timeCounterTextView = (TextView) findViewById(R.id.timeCounterTextView);
        LinearLayout cameraPreviewLinLayout = (LinearLayout) findViewById(R.id.cameraPreviewLinLayout);


        secondsLeft = 5;
        timeCounterTextView.setText(String.valueOf(secondsLeft));

        dbTopValue = getSharedPreferences(Constants.SHARED_PREF_FILENAME, Context.MODE_PRIVATE).getInt(Constants.SHARED_PREF_TOP_DB_VALUE, -1);
        isDbLevelReached = false;
        lastMeterDegree = 0f;
        moveMeterArrow(lastMeterDegree);

        soundLevelValue = new int[7];
        isApplicationInterrupted = false;

        // Create our Preview view and set it as the content of our activity.
        preview = new CameraPreview(this);
        List<Camera.Size> tmpList = preview.getCamera().getParameters().getSupportedPreviewSizes();
        RelativeLayout.LayoutParams cameraLP = (RelativeLayout.LayoutParams) cameraPreviewLinLayout.getLayoutParams();
        int maxWidthResolution = tmpList.get(0).width;
        int maxHeightResolution = tmpList.get(0).height;

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        //calculation of the best height and shift to the left
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

        //center time counter
        RelativeLayout.LayoutParams counterLP = (RelativeLayout.LayoutParams) timeCounterTextView.getLayoutParams();
        counterLP.topMargin = (screenHeight / 10) - (counterLP.height / 2);
        counterLP.leftMargin = (screenWidth / 4) - (counterLP.width / 2);
    }

    @Override
    protected void onStart() {
        super.onStart();
        disableAllAudio();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                preview.startRecording();
                isDbMeasuring = true;
                new TimerCountdown().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                new DecibelMeasuringTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }, 1000);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        isApplicationInterrupted = true;
        preview.stopRecording();
        isDbMeasuring = false;
        secondsLeft = -1;

        //String filePath = getSharedPreferences(Constants.SHARED_PREF_FILENAME, Context.MODE_PRIVATE).getString(Constants.SHARED_PREF_SAVED_VIDEO_PATH, null);
        //new File(filePath).delete();

        enableAllAudio();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        enableAllAudio();
    }

    private void disableAllAudio() {
        // disable sound when recording.
        AudioManager aManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        soundLevelValue[0] = aManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        soundLevelValue[1] = aManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        soundLevelValue[2] = aManager.getStreamVolume(AudioManager.STREAM_RING);
        soundLevelValue[3] = aManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        soundLevelValue[4] = aManager.getStreamVolume(AudioManager.STREAM_ALARM);
        soundLevelValue[5] = aManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        soundLevelValue[6] = aManager.getStreamVolume(AudioManager.STREAM_DTMF);

        aManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_DTMF, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    public void enableAllAudio() {
        // re-enable sound after recording.
        AudioManager aManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        aManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, soundLevelValue[0], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_SYSTEM, soundLevelValue[1], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_RING, soundLevelValue[2], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_MUSIC, soundLevelValue[3], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_ALARM, soundLevelValue[4], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, soundLevelValue[5], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        aManager.setStreamVolume(AudioManager.STREAM_DTMF, soundLevelValue[6], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    private void moveMeterArrow(float degree) {
        RotateAnimation animRotate = new RotateAnimation(lastMeterDegree, degree, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        animRotate.setDuration(0);
        animRotate.setFillEnabled(true);
        animRotate.setFillAfter(true);

        lastMeterDegree = degree;
        arrowImageView.startAnimation(animRotate);
    }

    public int getAmplitude() {
        if (preview.getMediaRecorder() != null) {
            return (preview.getMediaRecorder().getMaxAmplitude());
        } else {
            return 0;
        }
    }

    class DecibelMeasuringTask extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            while (isDbMeasuring) {
                try {
                    publishProgress((int) Math.round((87 + (20.0 * Math.log10((double) getAmplitude() / 32767)))));
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException | RuntimeException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... amplitude) {
            super.onProgressUpdate(amplitude);

            moveMeterArrow(amplitude[0] * 2);   //1 decibel == 2 degree on meter

            if (!isDbLevelReached) {
                isDbLevelReached = (amplitude[0] >= dbTopValue);
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
            while (secondsLeft >= 0) {
                try {
                    publishProgress(secondsLeft--);
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... seconds) {
            super.onProgressUpdate(seconds);
            timeCounterTextView.setText(String.valueOf(seconds[0]));
        }

        @Override
        protected void onPostExecute(Integer value) {
            super.onPostExecute(value);

            if (!isDbLevelReached && !isApplicationInterrupted) {
                isDbMeasuring = false;
                preview.stopRecording();

                Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                intent.putExtra(ResultActivity.RESULT_ID, ResultActivity.FAILURE);
                startActivity(intent);
                finish();
            } else if (isDbLevelReached && !isApplicationInterrupted) {
                isDbMeasuring = false;
                preview.stopRecording();

                Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                intent.putExtra(ResultActivity.RESULT_ID, ResultActivity.SUCCESS);
                startActivity(intent);
                finish();
            }
        }
    }
}
