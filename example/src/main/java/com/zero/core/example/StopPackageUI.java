package com.zero.core.example;

import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import com.zero.core.AppEnv;
import com.zero.core.Service;

/**
 * 测试例子
 *
 * @author chaopei
 */
public class StopPackageUI extends IStopPackage.Stub {

    private static final boolean DEBUG = AppEnv.DEBUG;

    private static final String TAG = StopPackageUI.class.getSimpleName();

    public static final String SERVICE_ID = "StopPackageUI";

    public static final Service INSTALLER = new Service(StopPackageUI.class) {

        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }

        @Override
        public String getProcessSuffix() {
            return ""; //主进程
        }

    };

    @Override
    public void killSysNoWait() throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killSysNoWait ui]");
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
    }

    @Override
    public void killSysWait() throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killSysWait ui]", new Throwable());
        }
//        int a=0;a=1/a;
        throw new NullPointerException();
    }

}
