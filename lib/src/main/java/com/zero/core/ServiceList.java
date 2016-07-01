package com.zero.core;

import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;
import android.util.SparseArray;


public class ServiceList {

    private static final boolean DEBUG = AppEnv.DEBUG;

    private static final String TAG = ServiceList.class.getSimpleName();

    static final int MIN_ID = 0;

    static final int MAX_ID = 1024;

    private static final SparseArray<Service> mAllServices = new SparseArray<Service>();

    private static SparseArray<IBinder> sCache = new SparseArray<IBinder>();


    /**
     * 【主进程】查询cache的Binder对象
     *
     * @param id
     * @return
     */
    static IBinder getCacheBinder(int id) {
        return sCache.get(id);
    }

    /**
     * 【主进程】往cache插入Binder对象
     *
     * @param id
     * @return
     */
    static void putCacheBinder(int id, IBinder binder) {
        sCache.put(id, binder);
    }

    /**
     * 【Server进程】获取install的Service
     *
     * @param id
     * @return
     */
    static Service getService(int id) {
        return mAllServices.get(id);
    }

    /**
     * 【Server进程】install时调用
     *
     * @param id
     * @param service
     */
    static void putService(int id, Service service) {
        mAllServices.put(id, service);
    }

    static IInterface getInterface(int id, IBinder binder) {
        Service service = mAllServices.get(id);
        if (service == null) {
            if (DEBUG) {
                Log.e(TAG, "[getInterface]：service is null, id=" + id);
            }
        }
        return mAllServices.get(id).asInterface(binder);
    }

    static void init() {
    }

}
