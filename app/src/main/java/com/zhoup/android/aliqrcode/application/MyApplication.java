package com.zhoup.android.aliqrcode.application;

import android.app.Application;
import android.content.Context;

import com.lzy.okgo.OkGo;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

/**
 * Created by zhoup on 2017/6/23.
 */

public class MyApplication extends Application {
    private RefWatcher mWatcher;

    //内存泄露检测
    public static RefWatcher getRefWatcher(Context context) {
        MyApplication application = (MyApplication) context.getApplicationContext();
        return application.mWatcher;
    }

    public static Context getApplicationContext(Context context) {
        return context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        OkGo.getInstance().init(this);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        mWatcher = LeakCanary.install(this);
    }
}
