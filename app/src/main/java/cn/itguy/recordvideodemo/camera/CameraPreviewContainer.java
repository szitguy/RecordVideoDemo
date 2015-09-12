package cn.itguy.recordvideodemo.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * 相机预览视图的容器视图
 *
 * @author Martin
 */
public class CameraPreviewContainer extends FrameLayout {

    private boolean isPortrait;

    public CameraPreviewContainer(Context context) {
        this(context, null);
    }

    public CameraPreviewContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreviewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 设置ViewGroup可绘制，这样就可以重写onDraw方法
        setWillNotDraw(false);
        isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 旋转90度
        if (isPortrait) {
//            canvas.ro
        }
    }
}
