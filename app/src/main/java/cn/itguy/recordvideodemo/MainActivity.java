package cn.itguy.recordvideodemo;

import android.app.Activity;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import cn.itguy.common.utils.FileUtil;
import cn.itguy.recordvideodemo.camera.CameraManager;
import cn.itguy.recordvideodemo.camera.CameraPreview;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;

    private CameraPreview mPreview;

    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CameraManager.checkCameraHardware(this)) {
            Toast.makeText(this, "找不到相机，3秒后退出！", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new FinishRunnable(this), 3000);
            return;
        }

        int cameraId = CameraManager.getDefaultCameraID();
        // Create an instance of Camera
        mCamera = CameraManager.getCameraInstance(cameraId);
        if (null == mCamera) {
            Toast.makeText(this, "打开相机失败！", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 设置相机参数
        Camera.Parameters parameters = mCamera.getParameters();
        // 若应用就是用来录制视频的，不用拍照功能，设置RecordingHint可以加快录制启动速度
        parameters.setRecordingHint(true);
        mCamera.setParameters(parameters);
        // 设置相机方向
        CameraManager.setCameraDisplayOrientation(this, cameraId, mCamera);

        setContentView(R.layout.activity_main);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        final FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        preview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                preview.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                ViewGroup.LayoutParams layoutParams = preview.getLayoutParams();
                layoutParams.height = (int) (3f / 4f * preview.getWidth());
                preview.setLayoutParams(layoutParams);

                ViewGroup.LayoutParams layoutParams2 = mPreview.getLayoutParams();
                layoutParams2.height = (int) (4f / 3f * preview.getWidth());
                mPreview.setLayoutParams(layoutParams2);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            // 锁相机，4.0以后系统自动管理调用，但若录制器prepare()方法失败，必须调用
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_start:
                startRecord();
                break;
            case R.id.button_stop:
                stopRecord();
                break;
        }
    }

    /**
     * 开始录制
     */
    private void startRecord() {
        if (isRecording) {
            Toast.makeText(this, "正在录制中…", Toast.LENGTH_SHORT).show();
            return;
        }

        // initialize video camera
        if (prepareVideoRecorder()) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mMediaRecorder.start();

            isRecording = true;
        } else {
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            // inform user
        }
    }

    /**
     * 准备视频录制器
     * @return
     */
    private boolean prepareVideoRecorder(){
        if (!FileUtil.isSDCardMounted()) {
            Toast.makeText(this, "SD卡不可用！", Toast.LENGTH_SHORT).show();
            return false;
        }

        File file = FileUtil.getOutputMediaFile(FileUtil.MEDIA_TYPE_VIDEO);
        if (null == file) {
            Toast.makeText(this, "创建存储文件失败！", Toast.LENGTH_SHORT).show();
            return false;
        }

        mMediaRecorder = new MediaRecorder();
        // Step 1: Unlock and set camera to MediaRecorder
        // 解锁相机以让其他进程能够访问相机，4.0以后系统自动管理调用，但是实际使用中，不调用的话，MediaRecorder.start()报错闪退
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_CIF));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(file.toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void stopRecord() {
        if (isRecording) {
            // stop recording and release camera
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            isRecording = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private static class FinishRunnable implements Runnable {

        private final WeakReference<Activity> mActivityWeakReference;

        public FinishRunnable(Activity activity) {
            mActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            Activity activity;
            if (null != (activity = mActivityWeakReference.get())) {
                activity.finish();
            }
        }
    }

}
