package com.blogspot.alex_dev.discountreaction.util;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.blogspot.alex_dev.discountreaction.activity.MeasureSoundActivity;

import java.io.IOException;

/**
 * A basic Camera preview class
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    public MediaRecorder mrec = new MediaRecorder();
    private static final String TAG = "Camera_log";

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        //mCamera.getParameters().setPreviewSize(512, 512    );
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            if (mCamera == null) {
                System.out.println("null");
                mCamera = MeasureSoundActivity.getCameraInstance();

            }
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public Camera getmCamera() {
        return mCamera;
    }

    public void setmCamera(Camera mCamera) {
        this.mCamera = mCamera;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        //Take care of releasing the Camera preview in your activity.
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private Camera.Size getBestPreviewSize(int width, int height) {
        Camera.Size result = null;
        Camera.Parameters p = mCamera.getParameters();
        for (Camera.Size size : p.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
//            Camera.Parameters parameters = mCamera.getParameters();
//            Camera.Size size = getBestPreviewSize(w, h);
//   //         parameters.setPreviewSize(size.width, size.height);
//            parameters.setPreviewSize(352, 288);
//            mCamera.setParameters(parameters);

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }


//    public void startRecording() throws IOException {
//        mrec = new MediaRecorder();  // Works well
//        mCamera.unlock();
//
//        mrec.setCamera(mCamera);
//
//        mrec.setPreviewDisplay(mHolder.getSurface());
//        mrec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        mrec.setAudioSource(MediaRecorder.AudioSource.MIC);
//
//        mrec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
//        mrec.setPreviewDisplay(mHolder.getSurface());
//        mrec.setOutputFile("/sdcard/zzzz.3gp");
//
//        mrec.prepare();
//        mrec.start();
//    }
//
//    public void stopRecording() {
//        mrec.stop();
//        mrec.release();
//        mCamera.release();
//    }
//
//    private void releaseMediaRecorder() {
//        if (mrec != null) {
//            mrec.reset();   // clear recorder configuration
//            mrec.release(); // release the recorder object
//            mrec = null;
//            mCamera.lock();           // lock camera for later use
//        }
//    }
//
//    private void releaseCamera() {
//        if (mCamera != null) {
//            mCamera.release();        // release the camera for other applications
//            mCamera = null;
//        }
//    }
}