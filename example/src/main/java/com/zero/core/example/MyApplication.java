package com.zero.core.example;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.zero.core.AppEnv;
import com.zero.core.AppUtil;

/**
 * Created by chaopei on 2016/7/2.
 * Application
 */
public class MyApplication extends Application {

    public static final boolean DEBUG = AppEnv.DEBUG;

    public static final String TAG = "MyApplication";

    static {
        if (DEBUG) {
            Log.d(TAG, "[static init]：running in process " + AppUtil.getProcessName());
        }
        StopPackageService.INSTALLER.install();
        StopPackageUI.INSTALLER.install();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        AppUtil.init(this); // needed!
    }

}
