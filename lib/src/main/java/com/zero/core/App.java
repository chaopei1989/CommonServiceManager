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
public class App extends Application {

    public static final boolean DEBUG = AppEnv.DEBUG;

    public static final String TAG = "App";
    /**
     * 未知进程
     */
    public static final int PROCESS_TYPE_UNKOWN = 0;
    /**
     * 服务进程
     */
    public static final int PROCESS_TYPE_SERVER = 1;
    /**
     * UI进程
     */
    public static final int PROCESS_TYPE_UI = 2;

    private static int sCurProcessType = PROCESS_TYPE_UNKOWN;

    private static App sInstance;

    public static App getContext() {
        return sInstance;
    }

    public static boolean runInServerProcess() {
        return sCurProcessType == PROCESS_TYPE_SERVER;
    }

    public static boolean runInUiProcess() {
        return sCurProcessType == PROCESS_TYPE_UI;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        sInstance = this;
        initProcessType();
    }

    private void initProcessType() {
        String process = getCurrentProcessName();
        if (TextUtils.isEmpty(process)
                || !process.startsWith("com.zero")) {
            sCurProcessType = PROCESS_TYPE_UNKOWN;
            Log.e(TAG, "PROCESS_TYPE_UNKOWN", new Exception());
        } else if (process.endsWith(":server")) {
            sCurProcessType = PROCESS_TYPE_SERVER;
        } else {
            sCurProcessType = PROCESS_TYPE_UI;
            if (DEBUG) {
                Log.d(TAG, "Process.myUid = " + android.os.Process.myUid());
            }
        }
    }

    /**
     * 返回当前的进程名
     */
    private static String getCurrentProcessName() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/self/cmdline")));
            String line = null;
            while ((line = reader.readLine()) != null) {
                return line.trim();
            }
        } catch (Exception e) {
            if (AppEnv.DEBUG)
                Log.e(TAG, "[getCurrentProcessName]: ", e);
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (Exception e) {
                }
        }
        return null;
    }

    public static String getProcessName() {
        String process = getCurrentProcessName();
        return process;
    }

    public static void ensureInServerProcess() {
        if (!runInServerProcess()) {
            throw new RuntimeException("please ensure In SProcess");
        }
    }
}
