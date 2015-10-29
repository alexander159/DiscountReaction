package com.blogspot.alex_dev.discountreaction.activity;

import android.os.Bundle;
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

            if (resCode == SUCCESS){
                resultImageView.setImageResource(R.drawable.screen03_text_succes);
            } else if (resCode == FAILURE){
                resultImageView.setImageResource(R.drawable.screen03_text_failure);
            }
        }

        //TODO show try again msg
    }
}
