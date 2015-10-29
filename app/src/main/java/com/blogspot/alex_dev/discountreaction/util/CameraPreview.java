package com.blogspot.alex_dev.discountreaction.util;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.blogspot.alex_dev.discountreaction.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    public MediaRecorder mrec;
    private boolean isRecording;
    private SurfaceHolder surfaceHolder;
    private Camera camera;

    public CameraPreview(Context context) {
        super(context);
        camera = getCameraInstance();

        mrec = new MediaRecorder();
        isRecording = false;

        // Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed.
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void startRecording() {
        if (!isRecording) {
            try {
                mrec = new MediaRecorder();  // Works well
                camera.unlock();

                mrec.setCamera(camera);

                mrec.setPreviewDisplay(surfaceHolder.getSurface());
                mrec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mrec.setAudioSource(MediaRecorder.AudioSource.MIC);
                mrec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");

                if (!mediaStorageDir.exists()) {
                    if (!mediaStorageDir.mkdirs()) {
                        Log.d("MyCameraApp", "failed to create directory");
                    }
                }

                File f = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");

                mrec.setOutputFile(f.getPath());

                mrec.prepare();
                mrec.start();

                isRecording = true;
            } catch (Exception e) {
                e.printStackTrace();
                mrec.release();
            }
        }
    }

    public void stopRecording() {
        if (isRecording) {
            isRecording = false;
            mrec.stop();
            mrec.release();
            //camera.release();
            mrec = null;
        }
    }

//    private void releaseMediaRecorder(){
//        if (mrec != null) {
//            mrec.reset();   // clear recorder configuration
//            mrec.release(); // release the recorder object
//            mrec = null;
//            camera.lock();           // lock camera for later use
//        }
//    }
//
//    private void releaseCamera(){
//        if (camera != null){
//            camera.release();        // release the camera for other applications
//            camera = null;
//        }
//    }

    /**
     * A safe way to get an instance of the Camera object.
     * @return Front camera instance if it is available, null otherwise
     */
    private Camera getCameraInstance() {
        int numCams = Camera.getNumberOfCameras();
        if (numCams == 0) return null;

        Camera c = null;
        try {
            c = android.hardware.Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } catch (Exception e) {
            Toast.makeText(getContext(), getContext().getString(R.string.camera_unavailable), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        return c;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        try {
            if (camera == null) {
                camera = getCameraInstance();
            }
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Take care of releasing the Camera preview in your activity.
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        try {
            if (mrec != null) {
                mrec.stop();
                mrec.release();
                mrec = null;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or reformatting changes here

        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public MediaRecorder getMediaRecorder() {
        return mrec;
    }
}