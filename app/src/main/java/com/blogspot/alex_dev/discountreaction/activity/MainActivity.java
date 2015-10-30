package com.blogspot.alex_dev.discountreaction.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.blogspot.alex_dev.discountreaction.R;
import com.blogspot.alex_dev.discountreaction.util.Constants;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private MediaRecorder mRecorder;
    private boolean isDbMeasuring;
    private TextView currentDbTextView;
    private int lastHighestDbValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lastHighestDbValue = 0;

        ImageButton startBtn = (ImageButton) findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //don't move next without entered db value
                if (getSharedPreferences(Constants.SHARED_PREF_FILENAME, Context.MODE_PRIVATE).getInt(Constants.SHARED_PREF_DB_VALUE, -1) == -1) {
                    Toast.makeText(getApplicationContext(), getString(R.string.enter_db), Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(getApplicationContext(), MeasureReactionActivity.class);
                startActivity(intent);
                finish();
            }
        });

        TextView settingsTextView = (TextView) findViewById(R.id.settingsTextView);
        settingsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();

                isDbMeasuring = true;
                new DbMeasuringTask().execute();
            }
        });

        if (getSharedPreferences(Constants.SHARED_PREF_FILENAME, Context.MODE_PRIVATE).getInt(Constants.SHARED_PREF_DB_VALUE, -1) == -1) {
            showInputDialog();

            isDbMeasuring = true;
            new DbMeasuringTask().execute();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //isDbMeasuring = true;
        startRecorder();
    }

    @Override
    public void onPause() {
        super.onPause();
        //isDbMeasuring = false;
        stopRecorder();
    }

    public void startRecorder() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(CamcorderProfile.get(CamcorderProfile.QUALITY_480P).fileFormat);
            mRecorder.setAudioEncoder(CamcorderProfile.get(CamcorderProfile.QUALITY_480P).audioCodec);
            mRecorder.setAudioSamplingRate(CamcorderProfile.get(CamcorderProfile.QUALITY_480P).audioSampleRate);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
            } catch (java.io.IOException ioe) {
                Log.e("[Monkey]", "IOException: " + Log.getStackTraceString(ioe));

            } catch (java.lang.SecurityException e) {
                Log.e("[Monkey]", "SecurityException: " + Log.getStackTraceString(e));
            }
            try {
                mRecorder.start();
            } catch (java.lang.SecurityException e) {
                Log.e("[Monkey]", "SecurityException: " + Log.getStackTraceString(e));
            }
        }
    }

    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return (mRecorder.getMaxAmplitude());
        else
            return 0;
    }

    protected void showInputDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View inputDialog = layoutInflater.inflate(R.layout.input_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(getString(R.string.enter_db))
                .setView(inputDialog);

        final EditText editText = (EditText) inputDialog.findViewById(R.id.editText);
        currentDbTextView = (TextView) inputDialog.findViewById(R.id.currentDbTextView);

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            SharedPreferences.Editor ed = getSharedPreferences(Constants.SHARED_PREF_FILENAME, MODE_PRIVATE).edit();

                            if (Integer.parseInt(editText.getText().toString()) > 327) {
                                ed.putInt(Constants.SHARED_PREF_DB_VALUE, 327);
                            } else {
                                ed.putInt(Constants.SHARED_PREF_DB_VALUE, Integer.parseInt(editText.getText().toString()));
                            }
                            ed.commit();

                            isDbMeasuring = false;
                        } catch (NumberFormatException e) {
                            Toast.makeText(getApplicationContext(), getString(R.string.wrong_db), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();

                                isDbMeasuring = false;
                            }
                        })
                .show();
    }

    class DbMeasuringTask extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            while (isDbMeasuring) {
                try {
                    publishProgress((int) (getAmplitude() / 100));
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException | RuntimeException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (lastHighestDbValue < values[0]) {
                lastHighestDbValue = values[0];
            }

            currentDbTextView.setText(String.format("Valor actual: %s (último máximo %s)", values[0], lastHighestDbValue));
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }
}