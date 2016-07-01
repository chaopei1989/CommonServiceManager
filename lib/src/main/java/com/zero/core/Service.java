package com.zero.core;

import android.os.IBinder;
import android.os.IInterface;

public abstract class Service {

    public abstract IInterface asInterface(IBinder binder);

    public abstract int getServiceId();

    /**
     * 【Server进程】
     * 
     * @return
     */
    public abstract IBinder getService();

    /**
     * install此Service到mAllServices中
     */
    public void install() {
        int id = getServiceId();
        if (id < ServiceList.MIN_ID || id > ServiceList.MAX_ID) {
            throw new IllegalArgumentException();
        }

        if (null == ServiceList.getService(id)) {
            ServiceList.putService(getServiceId(), this);
        }
    }
}