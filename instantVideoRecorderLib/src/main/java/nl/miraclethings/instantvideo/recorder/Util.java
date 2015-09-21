package nl.miraclethings.instantvideo.recorder;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Environment;
import android.view.Surface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class Util {

    public static String getRecordingTimeFromMillis(long millis) {
        String strRecordingTime = null;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;

        if (hours >= 0 && hours < 10)
            strRecordingTime = "0" + hours + ":";
        else
            strRecordingTime = hours + ":";

        if (hours > 0)
            minutes = minutes % 60;

        if (minutes >= 0 && minutes < 10)
            strRecordingTime += "0" + minutes + ":";
        else
            strRecordingTime += minutes + ":";

        seconds = seconds % 60;

        if (seconds >= 0 && seconds < 10)
            strRecordingTime += "0" + seconds;
        else
            strRecordingTime += seconds;

        return strRecordingTime;

    }

    public static int determineDisplayOrientation(Activity activity, int cameraId) {
        int displayOrientation = 0;

        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int degrees = getRotationAngle(activity);

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (cameraInfo.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return displayOrientation;
    }

    public static int getRotationAngle(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        return degrees;
    }

    public static String createFilePath(String folder, String subfolder, String uniqueId) {
        File dir = new File(Environment.getExternalStorageDirectory(), folder);
        if (subfolder != null) {
            dir = new File(dir, subfolder);
        }
        dir.mkdirs();
        String fileName = Constants.FILE_START_NAME + uniqueId + Constants.VIDEO_EXTENSION;
        return new File(dir,fileName).getAbsolutePath();
    }

    public static List<Camera.Size> getResolutionList(Camera camera) {
        Parameters parameters = camera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();


        return previewSizes;
    }

    public static RecorderParameters getRecorderParameter(int currentResolution) {
        RecorderParameters parameters = new RecorderParameters();
        if (currentResolution == Constants.RESOLUTION_HIGH_VALUE) {
            parameters.setAudioBitrate(128000);
            parameters.setVideoQuality(0);
        } else if (currentResolution == Constants.RESOLUTION_MEDIUM_VALUE) {
            parameters.setAudioBitrate(128000);
            parameters.setVideoQuality(20);
        } else if (currentResolution == Constants.RESOLUTION_LOW_VALUE) {
            parameters.setAudioBitrate(96000);
            parameters.setVideoQuality(32);
        }
        return parameters;
    }

    public static int calculateMargin(int previewWidth, int screenWidth) {
        int margin = 0;
        if (previewWidth <= Constants.RESOLUTION_LOW) {
            margin = (int) (screenWidth * 0.12);
        } else if (previewWidth > Constants.RESOLUTION_LOW && previewWidth <= Constants.RESOLUTION_MEDIUM) {
            margin = (int) (screenWidth * 0.08);
        } else if (previewWidth > Constants.RESOLUTION_MEDIUM && previewWidth <= Constants.RESOLUTION_HIGH) {
            margin = (int) (screenWidth * 0.08);
        }
        return margin;


    }

    public static class ResolutionComparator implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size size1, Camera.Size size2) {
            if (size1.height != size2.height)
                return size1.height - size2.height;
            else
                return size1.width - size2.width;
        }
    }

    public static int combineVideoAndAudio(Context paramContext, String mCurrentVideoOutput, String mAudioFilename, int orientation, String mOutput) {
        return (new Processor(getEncodingLibraryPath(paramContext), paramContext)).newCommand().addInputPath(mCurrentVideoOutput).addInputPath(mAudioFilename).setMap("0:0").setMap("1:0").setCopy().setMetaData(getMetaData()).setOrientation(orientation).enableOverwrite().processToOutput(mOutput);
    }

    public static String getEncodingLibraryPath(Context paramContext) {
        return paramContext.getApplicationInfo().nativeLibraryDir + "/libencoding.so";
    }

    private static HashMap<String, String> getMetaData() {
        HashMap<String, String> localHashMap = new HashMap<String, String>();
        localHashMap.put("creation_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new Date()));
        return localHashMap;
    }

    public static int getTimeStampInNsFromSampleCounted(int paramInt) {
        return (int) (paramInt / 0.0441D);
    }


}
