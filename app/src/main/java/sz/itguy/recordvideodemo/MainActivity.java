package sz.itguy.recordvideodemo;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import sz.itguy.recordvideodemo.camera.CameraPreview;
import sz.itguy.utils.FileUtil;
import sz.itguy.wxlikevideo.camera.CameraHelper;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    // 输出宽度
    private static final int OUTPUT_WIDTH = 320;
    // 输出高度
    private static final int OUTPUT_HEIGHT = 240;
    // 宽高比
    private static final float RATIO = 1f * OUTPUT_WIDTH / OUTPUT_HEIGHT;

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;

    private CameraPreview mPreview;

    private boolean isRecording = false;

    private Camera.Size mVideoSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CameraHelper.checkCameraHardware(this)) {
            Toast.makeText(this, "找不到相机，3秒后退出！", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new FinishRunnable(this), 3000);
            return;
        }

        int cameraId = CameraHelper.getDefaultCameraID();
        if (!CameraHelper.isCameraFacingBack(cameraId)) {
            Toast.makeText(this, "找不到后置相机，3秒后退出！", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new FinishRunnable(this), 3000);
            return;
        }
        // Create an instance of Camera
        mCamera = CameraHelper.getCameraInstance(cameraId);
        if (null == mCamera) {
            Toast.makeText(this, "打开相机失败！", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setupCamera(cameraId);

        setContentView(R.layout.activity_main);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        final FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        // 根据需要输出的视频大小调整预览视图高度
        preview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                preview.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                ViewGroup.LayoutParams layoutParams = preview.getLayoutParams();
                layoutParams.height = (int) (preview.getWidth() / RATIO);
                preview.setLayoutParams(layoutParams);
            }
        });

        findViewById(R.id.button_start).setOnTouchListener(new RecordButtonTouchListener(this));

        ((TextView) findViewById(R.id.filePathTextView)).setText("请在" + FileUtil.MEDIA_FILE_DIR + "查看录制的视频文件");

    }

    /**
     * 设置相机参数
     */
    private void setupCamera(int cameraId) {
        // 设置相机方向
        CameraHelper.setCameraDisplayOrientation(this, cameraId, mCamera);
        // 设置相机参数
        Camera.Parameters parameters = mCamera.getParameters();
        // 若应用就是用来录制视频的，不用拍照功能，设置RecordingHint可以加快录制启动速度
        // 问题：小米手机录制视频支持的Size和相机预览支持的Size不一样（其他类似的手机可能
        // 也存在这个问题），若设置了这个标志位，会使预览效果拉伸，但是开始录制视频，预览
        // 又恢复正常，暂时不知道是为什么
        parameters.setRecordingHint(true);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setFocusAreas(null);
        parameters.setMeteringAreas(null);
        mVideoSize = CameraHelper.getCameraPreviewSizeForVideo(cameraId, mCamera);
        parameters.setPreviewSize(mVideoSize.width, mVideoSize.height);
        mCamera.setParameters(parameters);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 页面销毁时要停止录制
        stopRecord();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
        finish();
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
            // 虽然我之前并没有setPreviewCallback，但不加这句的话，
            // 后面要用到Camera时，调用Camera随便一个方法，都会报
            // Method called after release() error闪退，推测可能
            // Camera内存泄露无法真正释放，加上这句可以规避该问题
            mCamera.setPreviewCallback(null);
            // 释放前先停止预览
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
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
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
//        mMediaRecorder.setVideoSize(mVideoSize.width, mVideoSize.height);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(file.toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        mMediaRecorder.setMaxDuration(6000);

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
            try {
                mMediaRecorder.stop();  // stop the recording
            } catch (Exception e) {
                // TODO 删除已经创建的视频文件
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            isRecording = false;
        }
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

    private static class RecordButtonTouchListener implements View.OnTouchListener {

        private static final int CANCEL_RECORD_OFFSET = -100;

        private float mDownX, mDownY;

        private WeakReference<MainActivity> mMainActivityWeakReference;

        private boolean isCancelRecord = false;
        private boolean jumpToNew = true;

        public RecordButtonTouchListener(MainActivity mainActivity) {
            mMainActivityWeakReference = new WeakReference<MainActivity>(mainActivity);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final MainActivity mainActivity = mMainActivityWeakReference.get();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isCancelRecord = false;
                    mDownX = event.getX();
                    mDownY = event.getY();
                    if (null != mainActivity)
                        mainActivity.startRecord();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float y = event.getY();
                    if (y - mDownY < CANCEL_RECORD_OFFSET) {
                        if (!isCancelRecord) {
                            // cancel record
                            isCancelRecord = true;
                            if (null != mainActivity)
                                Toast.makeText(mainActivity, "cancel record", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        isCancelRecord = false;
                    }

                    // 跳转到新的视频录制页面
                    if (jumpToNew && y - mDownY > -CANCEL_RECORD_OFFSET) {
                        if (null != mainActivity) {
                            mainActivity.startActivity(new Intent(mainActivity, NewRecordVideoActivity.class));
                        }
                        jumpToNew = false;
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    // cancel?
//                    if (isCancelRecord) {
//
//                    } else {
//
//                    }
                    if (null != mainActivity)
                        mainActivity.stopRecord();
                    break;
            }

            return true;
        }
    }

}
