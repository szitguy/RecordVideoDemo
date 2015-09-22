package sz.itguy.utils;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

/**
 * 系统版本工具类
 * @author Martin
 *
 */
public class SystemVersionUtil {

    /**
     * 判断版本>=2.2
     * @return
     */
    public static boolean hasFroyo() {
        return VERSION.SDK_INT >= VERSION_CODES.FROYO;
    }

    /**
     * 判断版本>=3.0
     * @return
     */
    public static boolean hasHoneycomb() {
        return VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;
    }

    /**
     * 判断版本>=4.0
     * @return
     */
    public static boolean hasICS() {
        return VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    /**
     * 判断版本>=4.1
     * @return
     */
    public static boolean hasJellyBean() {
        return VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;
    }

    /**
     * 判断版本>=4.2
     * @return
     */
    public static boolean hasJellyBeanMR1() {
        return VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1;
    }

    /**
     * 判断版本>=4.3
     * @return
     */
    public static boolean hasJellyBeanMR2() {
        return VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2;
    }

	/**
	 * 判断版本>=4.4
	 * @return
	 */
	public static boolean hasKitKat() {
		return VERSION.SDK_INT >= VERSION_CODES.KITKAT;
	}
	
	/**
	 * 判断版本>=5.0
	 * @return
	 */
	public static boolean hasLollipop() {
		return VERSION.SDK_INT >= 21;
	}

}
