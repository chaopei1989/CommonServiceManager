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
 */
public class WhiteListService extends IWhiteListImpl.Stub {

    private static final boolean DEBUG = AppEnv.DEBUG;

    private static final String TAG = WhiteListService.class.getSimpleName();

    public static final String SERVICE_ID = "WhiteListService";

    public static final Service INSTALLER = new Service(WhiteListService.class) {

        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }

    };

    @Override
    public boolean isWhite(String pkg) throws RemoteException {
        //todo
        return false;
    }
}
