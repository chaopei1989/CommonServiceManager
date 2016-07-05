package com.zero.core;

import android.os.IBinder;
import android.os.IInterface;
import android.text.TextUtils;

import java.util.IllegalFormatException;

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
     * 返回运行所在进程名的结尾，如果就是运行在server进程则不用覆写(默认返回null)，空字符代表主进程
     * 例如，运行在 [package]:float，那就 return ":float"，不能写错.
     * @return
     */
    public String getProcessSuffix() {
        return null;
    }

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

    public boolean isImplementProcess() {
        String suffix = getProcessSuffix();
        if (null == suffix) {
            return AppUtil.runInServerProcess();
        } else {
            if (!TextUtils.isEmpty(suffix)) {
                return AppUtil.getProcessName().endsWith(getProcessSuffix());
            } else {
                return AppUtil.getProcessName().equals(AppUtil.getApplication().getPackageName());
            }
        }
    }

    /**
     * 确保执行在 server 进程，如果不是则崩溃
     */
    public void ensureInRightProcess() {
        if (!isImplementProcess()) {
            throw new RuntimeException("please ensure In process "+ getProcessSuffix());
        }
    }
}