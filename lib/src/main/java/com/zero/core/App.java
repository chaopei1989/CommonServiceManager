package com.zero.core;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Created by chaopei on 2016/7/2.
 * 需要继承
 */
public abstract class App extends Application {

    public static final boolean DEBUG = AppEnv.DEBUG;

    public static final String TAG = "App";

    public static final String PROCESS_SERVER_SUFFIX = ":server";

    private static String sCurProcessName;

    private static App sInstance;

    public static App getContext() {
        return sInstance;
    }

    public static boolean runInServerProcess() {
        String p = getCurrentProcessName();
        return !TextUtils.isEmpty(p) && p.endsWith(PROCESS_SERVER_SUFFIX);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    /**
     * 返回当前的进程名
     */
    private static String getCurrentProcessName() {
        if (TextUtils.isEmpty(sCurProcessName)) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream("/proc/self/cmdline")));
                String line;
                while ((line = reader.readLine()) != null) {
                    sCurProcessName = line.trim();
                }
            } catch (Exception e) {
                if (AppEnv.DEBUG) {
                    Log.e(TAG, "[getCurrentProcessName]: ", e);
                }
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (Exception e) {
                    }
            }
        }
        return sCurProcessName;
    }

    public static String getProcessName() {
        String process = getCurrentProcessName();
        return process;
    }

    public static void ensureInServerProcess() {
        if (!runInServerProcess()) {
            throw new RuntimeException("please ensure In ServerProcess");
        }
    }
}
