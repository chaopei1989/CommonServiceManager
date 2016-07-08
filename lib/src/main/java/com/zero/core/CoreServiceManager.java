package com.zero.core;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileDescriptor;

public class CoreServiceManager {

    private static final boolean DEBUG = AppEnv.DEBUG;

    private static final String TAG = CoreServiceManager.class.getSimpleName();

    private static final Uri SERVICE_MANAGER_URI;

    static final String SERVICE_MANAGER_KEY = CoreProvider.PATH_SERVICE_PROVIDER;

    static {
        SERVICE_MANAGER_URI = Uri.parse("content://"
                + CoreProvider.AUTHORITY + "/"
                + CoreProvider.PATH_SERVICE_PROVIDER);
    }

    /**
     * 进程单例，没有其他地方赋值了
     */
    private static final CoreServiceManagerProxy CORE_SERVICE_MANAGER_PROXY = new CoreServiceManagerProxy();

    private static class CoreServiceManagerProxy implements
            ICoreServiceManager, IBinder.DeathRecipient {

        private ICoreServiceManager mBase;

        private IOtherServiceManager.Stub mOtherServiceManagerImpl;

        CoreServiceManagerProxy() {
            synchronized (this) {
                refreshServiceManager();
            }
        }

        private synchronized ICoreServiceManager getCoreServiceManagerImpl() {
            if (null == mBase) {
                refreshServiceManager();
            }
            return mBase;
        }

        private void refreshServiceManager() {
            if (DEBUG) {
                Log.d(TAG, "[refreshServiceManager]");
            }
            mBase = fetchLocked();
            if (null != mBase) {
                try {
                    mBase.asBinder().linkToDeath(this, 0);
                    if (!AppUtil.runInCoreProcess()) {
                        mBase.installOtherManager(AppUtil.getProcessName(), getOtherServiceManagerImpl());
                    }
                } catch (RemoteException e) {
                    if (DEBUG) {
                        Log.e(TAG, "[refreshServiceManager]：RemoteException", e);
                    }
                }
            }
        }

        /**
         * 通过ServiceManagerProvider获取服务管理对象
         *
         * @return
         */
        private synchronized ICoreServiceManager fetchLocked() {
            ICoreServiceManager service = null;

            try {
                Bundle bundle = AppUtil.getApplication().getContentResolver().call(SERVICE_MANAGER_URI, CoreProvider.PATH_SERVICE_PROVIDER, null, null);
                if (null != bundle) {
                    bundle.setClassLoader(ServiceParcel.class.getClassLoader());
                    ServiceParcel serviceParcel = bundle.getParcelable(SERVICE_MANAGER_KEY);
                    if (null != serviceParcel) {
                        IBinder binder = serviceParcel.getBinder();
                        service = ICoreServiceManager.Stub.asInterface(binder);
                    }
                }
            } catch (Exception e) {
                if (DEBUG) {
                    Log.e(TAG, "[fetchLocked]", e);
                }
            }
            return service;
        }

        @Override
        public IBinder asBinder() {
            ICoreServiceManager coreServiceChannel = getCoreServiceManagerImpl();
            if (coreServiceChannel != null) {
                return coreServiceChannel.asBinder();
            }
            return null;
        }

        /**
         * 返回的是传过来的原始binder对象
         */
        @Override
        public IBinder getCoreService(String serviceId) throws RemoteException {
            ICoreServiceManager service = getCoreServiceManagerImpl();
            if (service != null) {
                IBinder binder = service.getCoreService(serviceId);
                return binder;
            }
            return null;
        }

        @Override
        public void installOtherManager(String processName, IBinder other) throws RemoteException {
            ICoreServiceManager service = getCoreServiceManagerImpl();
            if (service != null) {
                service.installOtherManager(processName, other);
            }
        }

        @Override
        public IBinder getOtherManager(String processName) throws RemoteException {
            ICoreServiceManager service = getCoreServiceManagerImpl();
            if (service != null) {
                IBinder binder = service.getOtherManager(processName);
                if (null != binder) { //以processName做id
                    return binder;
                } else {
                    return null;
                }
            }
            return null;
        }

        @Override
        public synchronized void binderDied() {
            if (DEBUG) {
                Log.d(TAG, "[binderDied] service channel died, retried.");
            }
            refreshServiceManager();
        }

        private IOtherServiceManager.Stub getOtherServiceManagerImpl() {
            if (null == mOtherServiceManagerImpl) {
                mOtherServiceManagerImpl = new IOtherServiceManager.Stub() {

                    @Override
                    public IBinder getService(String id) throws RemoteException {
                        if (TextUtils.isEmpty(id)) {
                            throw new IllegalArgumentException();
                        }

                        Service serviceCreator = ServiceList.getService(id);
                        if (serviceCreator != null) {
                            return serviceCreator.getService();
                        } else {
                            if (DEBUG) {
                                Log.d(TAG, "[getOtherServiceManagerImpl] serviceCreator == null");
                            }
                        }

                        return null;
                    }
                };
            }
            return mOtherServiceManagerImpl;
        }
    }

    /**
     * 传过来的Binder对象的Wrapper，binder die 相关处理
     *
     * @author chaopei
     */
    private static class RemoteBinderProxy implements IBinder,
            IBinder.DeathRecipient {

        private IBinder mRemote;
        private String mServiceId;

        /**
         * 工厂方法，创建代理，给用户的接口都需要用此接口包装一下
         */
        public static IBinder createInterface(String serviceId, IBinder binder) {

            String descriptor = null;
            try {
                descriptor = binder.getInterfaceDescriptor();
            } catch (RemoteException e) {
            }
            android.os.IInterface iin = binder.queryLocalInterface(descriptor);
            if (((iin != null) && AppUtil.runInCoreProcess())) {
                return binder;
            }
            return new RemoteBinderProxy(serviceId, binder);
        }

        private RemoteBinderProxy(String id, IBinder binder) {
            mRemote = binder;
            mServiceId = id;
            try {
                mRemote.linkToDeath(this, 0);
            } catch (RemoteException e) {
                if (DEBUG) {
                    Log.e(TAG, "[ModuleChannelWrapper constructor]：RemoteException", e);
                }
            }
        }

        private IBinder getRemoteBinder() throws RemoteException {
            IBinder remote = mRemote;
            if (remote != null) {
                return remote;
            }
            remote = CORE_SERVICE_MANAGER_PROXY.getCoreService(mServiceId);
            if (remote == null) {
                throw new RemoteException();
            }
            return remote;
        }

        @Override
        public String getInterfaceDescriptor() throws RemoteException {
            return getRemoteBinder().getInterfaceDescriptor();
        }

        @Override
        public boolean pingBinder() {
            try {
                return getRemoteBinder().pingBinder();
            } catch (RemoteException e) {

            }
            return false;
        }

        @Override
        public boolean isBinderAlive() {
            try {
                return getRemoteBinder().isBinderAlive();
            } catch (RemoteException e) {
            }
            return false;
        }

        @Override
        public IInterface queryLocalInterface(String descriptor) {
            try {
                return getRemoteBinder().queryLocalInterface(descriptor);
            } catch (RemoteException e) {
            }
            return null;
        }

        @Override
        public void dump(FileDescriptor fd, String[] args)
                throws RemoteException {
            getRemoteBinder().dump(fd, args);
        }

        @Override
        public boolean transact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            return getRemoteBinder().transact(code, data, reply, flags);
        }

        @Override
        public void linkToDeath(DeathRecipient recipient, int flags)
                throws RemoteException {
            getRemoteBinder().linkToDeath(recipient, flags);
        }

        @Override
        public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
            try {
                return getRemoteBinder().unlinkToDeath(recipient, flags);
            } catch (Exception e) {
            }
            return false;
        }

        @Override
        public void binderDied() {
            if (DEBUG) {
                Log.d(TAG, "[binderDied]");
            }
            mRemote = null;
            ServiceList.removeCacheBinder(mServiceId);
        }

        public void dumpAsync(FileDescriptor fd, String[] args)
                throws RemoteException {
            if (DEBUG) {
                Log.d(TAG, "[dumpAsync]");
            }
        }

    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 对外接口，拿到IBinder后直接转接口，外部一律用此接口
     *
     * @param id
     * @return
     */
    public static IInterface getService(String id) {
        // 调用前一定会事先调用 Service.install 方法，每个进程都会预先执行一次。
        Service copy = ServiceList.getService(id);
        if (null == copy) {
            if (DEBUG) {
                Log.e(TAG, "[getService]：no such service.");
            }
            return null;
        }
        if (copy.isCurrImplementProcess()) {
            if (DEBUG) {
                Log.d(TAG, "[getService]：Run in impl process, return directly.");
            }
            return copy.asInterface(copy.getService());
        }


        IBinder binder = ServiceList.getCacheBinder(id);
        if (null == binder) {
            if (DEBUG) {
                Log.d(TAG, "[getService]：binder has no cache");
            }
            try {
                if (!copy.isImplementCoreProcess()) { // 非core接口
                    String processName = AppUtil.getPackageName() + copy.getProcessSuffix();
                    if (DEBUG) {
                        Log.d(TAG, "[getService]：not core impl, process=" + processName);
                    }
                    IOtherServiceManager manager = getOtherServiceManger(processName);
                    if (null != manager) {
                        binder = manager.getService(id);
                    }
                } else { //core接口
                    if (DEBUG) {
                        Log.d(TAG, "[getService]：core impl");
                    }
                    binder = CORE_SERVICE_MANAGER_PROXY.getCoreService(id);
                }
                if (null != binder) {
                    ServiceList.putCacheBinder(id, RemoteBinderProxy.createInterface(id, binder));
                }
            } catch (RemoteException e) {
                if (DEBUG) {
                    Log.e(TAG, "[getService]：RemoteException", e);
                }
            }
        } else {
            if (DEBUG) {
                Log.d(TAG, "[getService]：binder has cache");
            }
        }
        if (null == binder) {
            if (DEBUG) {
                Log.e(TAG, "[getService]：binder is null");
            }
            return null;
        }
        if (DEBUG) {
            Log.d(TAG, "[getService]：binder returned");
        }
        return ServiceList.getInterface(id, binder);
    }

    public static IOtherServiceManager getOtherServiceManger(String processName) {
        IBinder binder = ServiceList.getCacheBinder(processName);
        if (null == binder) {
            if (DEBUG) {
                Log.d(TAG, "[getOtherServiceManger]：binder has no cache");
            }
            try {
                binder = CORE_SERVICE_MANAGER_PROXY.getOtherManager(processName);
                if (null != binder) {
                    ServiceList.putCacheBinder(processName, RemoteBinderProxy.createInterface(processName, binder));
                }
            } catch (RemoteException e) {
                if (DEBUG) {
                    Log.e(TAG, "[getOtherServiceManger]：RemoteException", e);
                }
            }
        } else {
            if (DEBUG) {
                Log.d(TAG, "[getOtherServiceManger]：binder has cache");
            }
        }
        if (null == binder) {
            if (DEBUG) {
                Log.e(TAG, "[getOtherServiceManger]：binder is null");
            }
            return null;
        }
        if (DEBUG) {
            Log.d(TAG, "[getOtherServiceManger]：binder returned");
        }
        return IOtherServiceManager.Stub.asInterface(binder);
    }

}
