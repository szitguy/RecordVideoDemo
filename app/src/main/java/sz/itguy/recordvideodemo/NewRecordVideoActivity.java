package sz.itguy.recordvideodemo;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import sz.itguy.utils.FileUtil;
import sz.itguy.wxlikevideo.camera.CameraHelper;
import sz.itguy.wxlikevideo.recorder.WXLikeVideoRecorder;
import sz.itguy.wxlikevideo.views.CameraPreviewView;

/**
 * 新视频录制页面
 *
 * @author Martin
 */
public class NewRecordVideoActivity extends Activity implements View.OnTouchListener {

    private static final String TAG = "NewRecordVideoActivity";

    // 输出宽度
    private static final int OUTPUT_WIDTH = 320;
    // 输出高度
    private static final int OUTPUT_HEIGHT = 240;
    // 宽高比
    private static final float RATIO = 1f * OUTPUT_WIDTH / OUTPUT_HEIGHT;

    private Camera mCamera;

    private WXLikeVideoRecorder mRecorder;

    private static final int CANCEL_RECORD_OFFSET = -100;
    private float mDownX, mDownY;
    private boolean isCancelRecord = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int cameraId = CameraHelper.getDefaultCameraID();
        // Create an instance of Camera
        mCamera = CameraHelper.getCameraInstance(cameraId);
        if (null == mCamera) {
            Toast.makeText(this, "打开相机失败！", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 初始化相机
        setupCamera();
        CameraHelper.setCameraDisplayOrientation(this, cameraId, mCamera);
        // 初始化录像机
        mRecorder = new WXLikeVideoRecorder(this, FileUtil.MEDIA_FILE_DIR);
        mRecorder.setOutputSize(OUTPUT_WIDTH, OUTPUT_HEIGHT);

        setContentView(R.layout.activity_new_recorder);
        CameraPreviewView preview = (CameraPreviewView) findViewById(R.id.camera_preview);
        preview.setCamera(mCamera);

        mRecorder.setCameraPreviewView(preview);

        findViewById(R.id.button_start).setOnTouchListener(this);

        ((TextView) findViewById(R.id.filePathTextView)).setText("请在" + FileUtil.MEDIA_FILE_DIR + "查看录制的视频文件");
    }

    /**
     * 设置相机参数
     */
    private void setupCamera() {
        // 设置相机参数
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setFocusAreas(null);
        parameters.setMeteringAreas(null);
//        Camera.Size size = CameraHelper.getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), 1, 1);
//        parameters.setPreviewSize(size.width, size.height);
        mCamera.setParameters(parameters);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 页面销毁时要停止录制
        stopRecord();
        releaseCamera();              // release the camera immediately on pause event
        finish();
    }

    private void releaseCamera() {
        if (mCamera != null){
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
        if (mRecorder.isRecording()) {
            Toast.makeText(this, "正在录制中…", Toast.LENGTH_SHORT).show();
            return;
        }

        // initialize video camera
        if (prepareVideoRecorder()) {
            // 录制视频
            if (!mRecorder.startRecording())
                Toast.makeText(this, "录制失败…", Toast.LENGTH_SHORT).show();
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

        return true;
    }

    /**
     * 停止录制
     */
    private void stopRecord() {
        mRecorder.stopRecording();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isCancelRecord = false;
                    mDownX = event.getX();
                    mDownY = event.getY();
                    startRecord();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!mRecorder.isRecording())
                        return false;

                    float y = event.getY();
                    if (y - mDownY < CANCEL_RECORD_OFFSET) {
                        if (!isCancelRecord) {
                            // cancel record
                            isCancelRecord = true;
                            Toast.makeText(this, "cancel record", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        isCancelRecord = false;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    stopRecord();
                    break;
            }

        return true;
    }

    /**
     * 开始录制失败回调任务
     *
     * @author Martin
     */
    public static class StartRecordFailCallbackRunnable implements Runnable {

        private WeakReference<NewRecordVideoActivity> mNewRecordVideoActivityWeakReference;

        public StartRecordFailCallbackRunnable(NewRecordVideoActivity activity) {
            mNewRecordVideoActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            NewRecordVideoActivity activity;
            if (null == (activity = mNewRecordVideoActivityWeakReference.get()))
                return;

            String filePath = activity.mRecorder.getFilePath().getPath();
            if (!TextUtils.isEmpty(filePath)) {
                FileUtil.deleteFile(filePath);
                Toast.makeText(activity, "Start record failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 停止录制回调任务
     *
     * @author Martin
     */
    public static class StopRecordCallbackRunnable implements Runnable {

        private WeakReference<NewRecordVideoActivity> mNewRecordVideoActivityWeakReference;

        public StopRecordCallbackRunnable(NewRecordVideoActivity activity) {
            mNewRecordVideoActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            NewRecordVideoActivity activity;
            if (null == (activity = mNewRecordVideoActivityWeakReference.get()))
                return;

            String filePath = activity.mRecorder.getFilePath().getPath();
            if (!TextUtils.isEmpty(filePath)) {
                if (activity.isCancelRecord) {
                    FileUtil.deleteFile(filePath);
                } else {
                    Toast.makeText(activity, "Video file path: " + filePath, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
