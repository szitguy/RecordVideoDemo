package sz.itguy.recordvideodemo;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

/**
 * Created by Administrator on 2015/9/11.
 */
public class DemoApplication extends Application {

    public static RefWatcher getRefWatcher(Context context) {
        DemoApplication application = (DemoApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    private RefWatcher refWatcher;

    @Override public void onCreate() {
        super.onCreate();
        refWatcher = LeakCanary.install(this);
    }
}
