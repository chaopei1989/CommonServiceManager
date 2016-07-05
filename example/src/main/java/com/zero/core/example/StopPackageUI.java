package com.zero.core.example;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;

import com.zero.core.AppEnv;
import com.zero.core.AppUtil;
import com.zero.core.Service;

/**
 * 测试例子
 * 
 * @author chaopei
 *
 */
public class StopPackageUI extends IStopPackageService.Stub {

    private static final boolean DEBUG = AppEnv.DEBUG;

    private static final String TAG = StopPackageUI.class.getSimpleName();

    public static final int SERVICE_ID = 1;

    private static StopPackageUI instance;

    final public static Service INSTALLER = new Service() {

        @Override
        public int getServiceId() {
            return SERVICE_ID;
        }

        @Override
        public IBinder getService() {
            return StopPackageUI.getService();
        }

        @Override
        public String getProcessSuffix() {
            return "";//主进程
        }

        @Override
        public IInterface asInterface(IBinder binder) {
            return Stub.asInterface(binder);
        }
    };

    private StopPackageUI() {
    }

    /**
     * 【Server进程】install服务时使用
     * 
     * @return
     */
    public static IBinder getService() {
        INSTALLER.ensureInRightProcess();
        if (null == instance) {
            instance = new StopPackageUI();
        }
        return instance;
    }

    @Override
    public void killNoWait(String name) throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killNoWait ui]：" + name);
        }
    }

    @Override
    public void killWait(String name) throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killWait ui]：" + name);
        }

    }

    @Override
    public void killAllNoWait(String[] names) throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killAllNoWait ui]：" + names.length);
        }

    }

    @Override
    public void killAllWait(String[] names) throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killAllWait ui]：" + names.length);
        }

    }

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

    @Override
    public void killUserNoWait() throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killUserNoWait ui]");
        }

    }

    @Override
    public void killUserWait(Surface a) throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killUserWait ui]");
        }
    }

}
