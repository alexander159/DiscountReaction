package com.blogspot.alex_dev.discountreaction.activity;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.blogspot.alex_dev.discountreaction.R;
import com.blogspot.alex_dev.discountreaction.util.CameraPreview;

import java.util.List;

public class MeasureSoundActivity extends AppCompatActivity {
    private ImageView arrowImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_sound);

        arrowImageView = (ImageView) findViewById(R.id.arrowImageView);

        moveMeterArrow(0);

        // Create an instance of Camera
        Camera mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

        List<Camera.Size> tmpList = mCamera.getParameters().getSupportedPreviewSizes();
        LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams) preview.getLayoutParams();
        ll.width = tmpList.get(0).width;
        ll.height = tmpList.get(0).height;

        int screenWidth = 1920;
        int screenHeight = 1080;

        if (ll.height < screenHeight){
            int scale = screenHeight - ll.height;
            ll.height += scale;
            ll.width += scale;
        }

        preview.setLayoutParams(ll);

        ScrollView scrollView = (ScrollView)findViewById(R.id.scrollView);
        //scrollView.invalidate();
        //todo fix scale bug

        preview.addView(mPreview);
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = android.hardware.Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); // attempt to get a Camera instance
        }
        catch (Exception e){
            e.printStackTrace();
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void moveMeterArrow(float degree){
        RotateAnimation animRotate = new RotateAnimation(0.0f, degree, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        animRotate.setDuration(0);
        animRotate.setFillAfter(true);
        animRotate.setFillEnabled(true);

        arrowImageView.startAnimation(animRotate);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
