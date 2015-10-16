package sz.itguy.wxlikevideo.camera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.util.List;

/**
 * Created by Administrator on 2015/9/10.
 */
public class CameraHelper {

    private static final String TAG = "CameraHelper";

    public static int getAvailableCamerasCount() {
        return Camera.getNumberOfCameras();
    }

    /**
     * 获取默认（背部）相机id
     * @return
     */
    public static int getDefaultCameraID() {
        int camerasCnt = getAvailableCamerasCount();
        int defaultCameraID = -1;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i=0; i <camerasCnt; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                defaultCameraID = i;
            }
        }
        return defaultCameraID;
    }

    /**
     * 获取前置相机id
     * @return
     */
    public static int getFrontCameraID() {
        int camerasCnt = getAvailableCamerasCount();
        int defaultCameraID = -1;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i=0; i <camerasCnt; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                defaultCameraID = i;
            }
        }
        return defaultCameraID;
    }

    public static boolean isCameraFacingBack(int cameraID) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraID, cameraInfo);
        return (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static List<Camera.Size> getCameraSupportedVideoSizes(Camera camera) {
        if ((Build.VERSION.SDK_INT >= 11) && (camera != null)) {
//			return camera.getParameters().getSupportedVideoSizes();
            List<Camera.Size> sizes = camera.getParameters().getSupportedVideoSizes();
            if (sizes == null)
                return camera.getParameters().getSupportedPreviewSizes();
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * 根据相机id获取相机对象
     * @param cameraId
     * @return
     */
    public static Camera getCameraInstance(int cameraId){
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.e(TAG, "open camera failed: " + e.getMessage());
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /** Check if this device has a camera */
    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static int setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        Log.d(TAG, "camera display orientation: " + result);
        camera.setDisplayOrientation(result);

        return result;
    }

    /**
     * 获取最优预览尺寸
     * @param sizes
     * @param targetHeight
     * @return
     */
    public static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int targetHeight) {
        final double MIN_ASPECT_RATIO = 1.0;
        final double MAX_ASPECT_RATIO = 1.5;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (ratio <= MIN_ASPECT_RATIO || ratio > MAX_ASPECT_RATIO)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

//    /**
//     * 获取最优预览尺寸
//     * @param sizes
//     * @param w
//     * @param h
//     * @return
//     */
//    public static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
//        final double ASPECT_TOLERANCE = 0.1;
//        double targetRatio = (double) w / h;
//        if (sizes == null) {
//            return null;
//        }
//        Camera.Size optimalSize = null;
//        double minDiff = Double.MAX_VALUE;
//        int targetHeight = h;
//        for (Camera.Size size : sizes) {
//            double ratio = (double) size.width / size.height;
//            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
//                continue;
//            if (Math.abs(size.height - targetHeight) < minDiff) {
//                optimalSize = size;
//                minDiff = Math.abs(size.height - targetHeight);
//            }
//        }
//        if (optimalSize == null) {
//            minDiff = Double.MAX_VALUE;
//            for (Camera.Size size : sizes) {
//                if (Math.abs(size.height - targetHeight) < minDiff) {
//                    optimalSize = size;
//                    minDiff = Math.abs(size.height - targetHeight);
//                }
//            }
//        }
//        return optimalSize;
//    }

    /**
     * 获取相机录制CIF质量视频的宽高
     * @param cameraId
     * @param camera
     * @return
     */
    public static Camera.Size getCameraPreviewSizeForVideo(int cameraId, Camera camera) {
        CamcorderProfile cameraProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        return camera.new Size(cameraProfile.videoFrameWidth, cameraProfile.videoFrameHeight);



//        Camera.Parameters parameters = camera.getParameters();
//        List<Camera.Size> supportedVideoSizeList = parameters.getSupportedVideoSizes();
//        if (supportedVideoSizeList == null) {
//            supportedVideoSizeList = parameters.getSupportedPreviewSizes();
//        }
////        for (Camera.Size size : supportedVideoSizeList) {
////        }
//        return supportedVideoSizeList.get(supportedVideoSizeList.size() - 4);



//        Camera.Size currentSize = parameters.getPreviewSize();
//        Log.d(TAG, "current camera preview size w: " + currentSize.width + "---h: " + currentSize.height);
//
//        Camera.Size willSetSize = currentSize;
//        Camera.Size tempSize = null;
//        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
//        for (Camera.Size size : sizeList) {
//            Log.d(TAG, "supported camera preview size w: " + size.width + "---h: " + size.height);
//            // 如果宽高符合4:3要求，并且宽度比之前获得的宽度大，则取当前这个
//            if (1.0f * size.width / size.height == ratio) {
//                if (tempSize == null || size.width >= tempSize.width) {
//                    tempSize = size;
//                }
//            }
//        }
//
//        if (tempSize != null)
//            willSetSize = tempSize;
//
//        return willSetSize;
    }

    /**
     * 设置相机对焦模式
     *
     * @param focusMode
     * @param camera
     */
    public static void setCameraFocusMode(String focusMode, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<String> sfm = parameters.getSupportedFocusModes();
        if (sfm.contains(focusMode)) {
            parameters.setFocusMode(focusMode);
        }
        camera.setParameters(parameters);
    }

}
