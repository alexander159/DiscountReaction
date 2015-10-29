package com.blogspot.alex_dev.discountreaction.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.blogspot.alex_dev.discountreaction.R;

public class ResultActivity extends AppCompatActivity {
    public static final String RESULT_ID = "result";
    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ImageView resultImageView = (ImageView) findViewById(R.id.resultImageView);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int resCode = extras.getInt(RESULT_ID);

            if (resCode == SUCCESS) {
                resultImageView.setImageResource(R.drawable.screen03_text_succes);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showSaveRecordingDialog();
                    }
                }, 2000);   //show result screen during 2 sec
            } else if (resCode == FAILURE) {
                resultImageView.setImageResource(R.drawable.screen03_text_failure);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }, 3000);   //show result screen during 3 sec
            }
        }
    }

    private void showSaveRecordingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage(getString(R.string.save_recording));
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }
}
