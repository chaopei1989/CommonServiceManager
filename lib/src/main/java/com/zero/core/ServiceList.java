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

    /**
     * 非 core 进程注册的 ServiceManager
     */
    private static final HashMap<String, IBinder> OTHER_SERVICE_MANAGERS = new HashMap<String, IBinder>();

    private static final ConcurrentHashMap<String, Service> ALL_SERVICES = new ConcurrentHashMap<String, Service>();

    /**
     * 访问的接口cache
     */
    private static final ConcurrentHashMap<String, IBinder> BINDER_CACHE = new ConcurrentHashMap<String, IBinder>();

    static synchronized void putOtherManager(String process, IBinder binder) {
        if (DEBUG) {
            Log.d(TAG, "[putOtherManager] process=" + process);
        }
        OTHER_SERVICE_MANAGERS.put(process, binder);
    }

    static synchronized IBinder getOtherAvailableManager(String process) {
        if (DEBUG) {
            Log.d(TAG, "[getOtherAvailableManager] process=" + process);
        }
        IBinder manager = OTHER_SERVICE_MANAGERS.get(process);
        if (null != manager && manager.pingBinder()) {
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
    static IBinder getCacheBinder(String id) {
        return BINDER_CACHE.get(id);
    }

    /**
     * 【主进程】往cache插入Binder对象
     *
     * @param id
     * @return
     */
    static void putCacheBinder(String id, IBinder binder) {
        BINDER_CACHE.put(id, binder);
    }

    static void removeCacheBinder(String id) {
        BINDER_CACHE.remove(id);
    }

    /**
     * 【core 进程】获取install的Service
     *
     * @param id
     * @return
     */
    static Service getService(String id) {
        return ALL_SERVICES.get(id);
    }

    /**
     * 【core 进程】install时调用
     *
     * @param id
     * @param service
     */
    static void putService(String id, Service service) {
        ALL_SERVICES.put(id, service);
    }

    static IInterface getInterface(String id, IBinder binder) {
        Service service = ALL_SERVICES.get(id);
        if (service == null) {
            if (DEBUG) {
                Log.e(TAG, "[getInterface]：service is null, id=" + id);
            }
        }
        return ALL_SERVICES.get(id).asInterface(binder);
    }

}
