package com.zero.core;

import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务的实现列表
 */
public class ServiceList {

    private static final boolean DEBUG = AppEnv.DEBUG;

    private static final String TAG = ServiceList.class.getSimpleName();

    static final int MIN_ID = 0;

    static final int MAX_ID = 1024;

    /**
     * 非server进程注册的 ServiceManager
     */
    private static final HashMap<String, IOtherServiceManager> OTHER_SERVICE_MANAGERS = new HashMap<String, IOtherServiceManager>();

    private static final ConcurrentHashMap<Integer, Service> ALL_SERVICES = new ConcurrentHashMap<Integer, Service>();

    /**
     * 访问的接口cache
     */
    private static final ConcurrentHashMap<Integer, IBinder> BINDER_CACHE = new ConcurrentHashMap<Integer, IBinder>();


    static synchronized void putOtherManager(String process, IBinder binder) {
        if (DEBUG) {
            Log.d(TAG, "[putOtherManager] process=" + process);
        }
        OTHER_SERVICE_MANAGERS.put(process, IOtherServiceManager.Stub.asInterface(binder));
    }

    static synchronized IOtherServiceManager getOtherAvailableManager(String process) {
        if (DEBUG) {
            Log.d(TAG, "[getOtherAvailableManager] process=" + process);
        }
        IOtherServiceManager manager = OTHER_SERVICE_MANAGERS.get(process);
        if (null != manager && manager.asBinder().pingBinder()) {
            return manager;
        } else {
            OTHER_SERVICE_MANAGERS.remove(process);
            return null;
        }
    }

    /**
     * 【主进程】查询cache的Binder对象
     *
     * @param id
     * @return
     */
    static IBinder getCacheBinder(int id) {
        return BINDER_CACHE.get(id);
    }

    /**
     * 【主进程】往cache插入Binder对象
     *
     * @param id
     * @return
     */
    static void putCacheBinder(int id, IBinder binder) {
        BINDER_CACHE.put(id, binder);
    }

    static void removeCacheBinder(int id) {
        BINDER_CACHE.remove(id);
    }

    /**
     * 【Server进程】获取install的Service
     *
     * @param id
     * @return
     */
    static Service getService(int id) {
        return ALL_SERVICES.get(id);
    }

    /**
     * 【Server进程】install时调用
     *
     * @param id
     * @param service
     */
    static void putService(int id, Service service) {
        ALL_SERVICES.put(id, service);
    }

    static IInterface getInterface(int id, IBinder binder) {
        Service service = ALL_SERVICES.get(id);
        if (service == null) {
            if (DEBUG) {
                Log.e(TAG, "[getInterface]：service is null, id=" + id);
            }
        }
        return ALL_SERVICES.get(id).asInterface(binder);
    }

}
