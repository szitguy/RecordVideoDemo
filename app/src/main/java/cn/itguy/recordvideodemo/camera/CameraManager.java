package cn.itguy.recordvideodemo.camera;

import android.hardware.Camera;

/**
 * Created by Administrator on 2015/9/10.
 */
public class CameraManager {

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

}
