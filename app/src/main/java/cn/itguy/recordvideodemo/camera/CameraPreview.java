package cn.itguy.recordvideodemo.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraPreview";

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
//        // deprecated setting, but required on Android versions prior to 3.0
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // 因为surfaceChanged方法至少会调用一次，这里开始预览的话
        // 待会儿马上又会停止，然后再开启预览，所以可以不在这里开
        // 启预览
        // The Surface has been created, now tell the camera where to draw the preview.
//        try {
//            mCamera.setPreviewDisplay(holder);
//            mCamera.startPreview();
//        } catch (IOException e) {
//            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
//        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged w: " + w + "---h: " + h);

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size currentSize = parameters.getPreviewSize();
            Log.d(TAG, "current camera preview size w: " + currentSize.width + "---h: " + currentSize.height);

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            Log.d(TAG, "screenWidth: " + screenWidth);

            Camera.Size willSetSize = currentSize;
            Camera.Size tempSize = null;
            List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
            for (Camera.Size size : sizeList) {
                Log.d(TAG, "supported camera preview size w: " + size.width + "---h: " + size.height);
                // 如果宽高符合4:3要求，并且宽度比之前获得的宽度大，则取当前这个
                if (3 * size.width == 4 * size.height) {
                    if (tempSize == null || size.width >= tempSize.width) {
                        tempSize = size;
                    }
                }
            }

            if (tempSize != null)
                willSetSize = tempSize;

            parameters.setPreviewSize(willSetSize.width, willSetSize.height);
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            Log.d(TAG, "Error setting camera preview parameters: " + e.getMessage());
        }

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}