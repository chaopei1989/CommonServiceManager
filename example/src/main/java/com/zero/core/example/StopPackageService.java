package com.zero.core.example;

import android.os.RemoteException;
import android.util.Log;

import com.zero.core.AppEnv;
import com.zero.core.CoreServiceManager;
import com.zero.core.Service;

/**
 * 测试例子
 * 
 * @author chaopei
 *
 */
public class StopPackageService extends IStopPackage.Stub {

    private static final boolean DEBUG = AppEnv.DEBUG;

    private static final String TAG = StopPackageService.class.getSimpleName();

    public static final int SERVICE_ID = 0;

    final public static Service INSTALLER = new Service(StopPackageService.class) {

        @Override
        public int getServiceId() {
            return SERVICE_ID;
        }

    };

    @Override
    public void killSysNoWait() throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killSysNoWait]");
        }
        IStopPackage ui = (IStopPackage) CoreServiceManager.getService(StopPackageUI.SERVICE_ID);
        ui.killSysNoWait();
    }

    @Override
    public void killSysWait() throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "[killSysWait]", new Throwable());
        }
//        int a=0;a=1/a;
        throw new NullPointerException();
    }

}
