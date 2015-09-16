package cn.itguy.recordvideodemo;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

import cn.itguy.common.utils.FileUtil;
import cn.itguy.recordvideodemo.camera.CameraHelper;
import cn.itguy.recordvideodemo.camera.NewCameraPreview;

/**
 * 新视频录制页面
 *
 * @author Martin
 */
public class NewRecordVideoActivity extends Activity {

    private static final String TAG = "NewRecordVideoActivity";

    // 输出宽度
    private static final int OUTPUT_WIDTH = 320;
    // 输出高度
    private static final int OUTPUT_HEIGHT = 240;
    // 宽高比
    private static final float RATIO = 1f * OUTPUT_WIDTH / OUTPUT_HEIGHT;

    private Camera mCamera;
    private NewCameraPreview mPreview;
    private boolean isRecording;

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
        setupCamera(cameraId);

        setContentView(R.layout.activity_main);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new NewCameraPreview(this, mCamera);
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

        ((TextView) findViewById(R.id.filePathTextView)).setText("请在" + FileUtil.FILE_DIR + "查看录制的视频文件");

    }

    /**
     * 设置相机参数
     */
    private void setupCamera(int cameraId) {
        // 设置相机方向
        CameraHelper.setCameraDisplayOrientation(this, cameraId, mCamera);
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
        if (isRecording) {
            Toast.makeText(this, "正在录制中…", Toast.LENGTH_SHORT).show();
            return;
        }

        // initialize video camera
        if (prepareVideoRecorder()) {
            // TODO 录制视频
            isRecording = true;
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

        return true;
    }

    /**
     * 停止录制
     */
    private void stopRecord() {
        if (isRecording) {
            // TODO 停止录制
            isRecording = false;
        }
    }

    private static class RecordButtonTouchListener implements View.OnTouchListener {

        private static final int CANCEL_RECORD_OFFSET = -100;

        private float mDownX, mDownY;

        private WeakReference<NewRecordVideoActivity> mMainActivityWeakReference;

        private boolean isCancelRecord = false;

        public RecordButtonTouchListener(NewRecordVideoActivity newRecordVideoActivity) {
            mMainActivityWeakReference = new WeakReference<>(newRecordVideoActivity);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
//            NewRecordVideoActivity mainActivity = mMainActivityWeakReference.get();
//            switch (event.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    isCancelRecord = false;
//                    mDownX = event.getX();
//                    mDownY = event.getY();
//                    if (null != mainActivity)
//                        mainActivity.startRecord();
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    float y = event.getY();
//                    if (y - mDownY < CANCEL_RECORD_OFFSET) {
//                        if (!isCancelRecord) {
//                            // cancel record
//                            isCancelRecord = true;
//                            if (null != mainActivity)
//                                Toast.makeText(mainActivity, "cancel record", Toast.LENGTH_SHORT).show();
//                        }
//                    } else {
//                        isCancelRecord = false;
//                    }
//
//                    if (y - mDownY > -CANCEL_RECORD_OFFSET) {
//                        if (null != mainActivity)
//                            mainActivity.startActivity(new Intent(mainActivity, NewRecordVideoActivity.class));
//                    }
//
//                    break;
//                case MotionEvent.ACTION_UP:
//                    // cancel?
////                    if (isCancelRecord) {
////
////                    } else {
////
////                    }
//                    if (null != mainActivity)
//                        mainActivity.stopRecord();
//                    break;
//            }

            return true;
        }
    }
}
