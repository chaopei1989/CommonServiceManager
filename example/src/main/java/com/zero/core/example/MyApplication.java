package com.zero.core.example;

import android.util.Log;

import com.zero.core.App;

/**
 * Created by chaopei on 2016/7/2.
 */
public class MyApplication extends App {

    static {
        if (DEBUG) {
            Log.d(TAG, "[static init]：running in process " + App.getProcessName());
        }
        StopPackageService.INSTALLER.install();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
