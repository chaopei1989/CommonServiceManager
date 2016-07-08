package com.zero.core;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Created by chaopei on 2016/7/2.
 * Application 工具类
 */
public class AppUtil {

    public static final boolean DEBUG = AppEnv.DEBUG;

    public static final String TAG = "AppUtil";

    private static final int PROCESS_TYPE_UNKNOWN = 0;

    private static final int PROCESS_TYPE_CORE = 1;

    private static int sProcessType = PROCESS_TYPE_UNKNOWN;

    private static String sCurProcessName;

    private static String sPackageName;

    private static Application sInstance;

    public static Application getApplication() {
        return sInstance;
    }

    public static String getPackageName() {
        if (null == sPackageName) {
            sPackageName = getApplication().getPackageName();
        }
        return sPackageName;
    }

    /**
     * 一般在Application onCreate 时使用，必须调用的方法
     *
     * @param app
     */
    public static void init(Application app) {
        sInstance = app;
        if (!runInCoreProcess()) {
            CoreServiceManager.init();
        }
    }

    public static boolean runInCoreProcess() {
        return sProcessType == PROCESS_TYPE_CORE;
    }

    static void initCoreProcess(boolean isCore) {
        if (DEBUG) {
            Log.d(TAG, "[initCoreProcess]：running in process " + getProcessName());
        }
        if (isCore) {
            sProcessType = PROCESS_TYPE_CORE;
        }
    }

    private static String readProcessNameFromCmdline() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/self/cmdline"), Charset.defaultCharset()));
            String line = reader.readLine();
            if (null != line) {
                return line.trim();
            }
        } catch (Exception e) {
            if (AppEnv.DEBUG) {
                Log.e(TAG, "[getCurrentProcessName]: ", e);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
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

}
