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
public class AppUtil {

    public static final boolean DEBUG = AppEnv.DEBUG;

    public static final String TAG = "AppUtil";

    private static final int PROCESS_TYPE_UNKNOWN = 0;

    private static final int PROCESS_TYPE_SERVER = 1;

    public static int sProcessType = PROCESS_TYPE_UNKNOWN;

    private static String sCurProcessName;

    private static Application sInstance;

    public static Application getApplication() {
        return sInstance;
    }

    /**
     * 一般在Application onCreate 时使用，必须调用的方法
     * @param app
     */
    public static void init(Application app) {
        sInstance = app;
        if (!runInServerProcess()) {
            // todo
        }
    }

    public static boolean runInServerProcess() {
        return sProcessType == PROCESS_TYPE_SERVER;
    }

    static void initServerProcess(boolean isServer) {
        if (DEBUG) {
            Log.d(TAG, "[initServerProcess]：running in process " + getProcessName());
        }
        if (isServer) {
            sProcessType = PROCESS_TYPE_SERVER;
        }
    }

    private static String readProcessNameFromCmdline() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/self/cmdline")));
            String line;
            if ((line = reader.readLine()) != null) {
                return line.trim();
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
        return null;
    }

    /**
     * 返回当前的进程名
     */
    public static String getProcessName() {
        if (TextUtils.isEmpty(sCurProcessName)) {
            sCurProcessName = readProcessNameFromCmdline();
        }
        return sCurProcessName;
    }

    /**
     * 确保执行在 server 进程，如果不是则崩溃
     */
    public static void ensureInServerProcess() {
        if (!runInServerProcess()) {
            throw new RuntimeException("please ensure In ServerProcess");
        }
    }
}
