package com.zero.core;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
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
    private static final CoreServiceManagerProxy CORE_SERVICE_MANAGER_WRAPPER = new CoreServiceManagerProxy();

    private static class CoreServiceManagerProxy implements
            ICoreServiceManager, IBinder.DeathRecipient {

        private ICoreServiceManager mBase;

        private IOtherServiceManager.Stub mOtherServiceManagerImpl;

        private volatile int retriedCount = 0;

        private synchronized ICoreServiceManager getCoreServerManagerImpl() {
            if (null == mBase) {
                refreshServiceManager();
            }
            return mBase;
        }

        private void refreshServiceManager() {
            mBase = fetchLocked();
            if (null != mBase) {
                try {
                    mBase.asBinder().linkToDeath(this, 0);
                    if (!AppUtil.runInServerProcess()) {
                        mBase.installOtherManager(getOtherServiceManagerImpl());
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

        /**
         * 返回的是传过来的原始binder对象
         *
         * @param serviceId
         * @return
         * @throws RemoteException
         */
        private IBinder getOriginalService(int serviceId) throws RemoteException {
            ICoreServiceManager service = getCoreServerManagerImpl();
            if (service != null) {
                IBinder binder = service.getService(serviceId);
                return binder;
            }
            return null;
        }

        @Override
        public IBinder asBinder() {
            ICoreServiceManager serverChannel = getCoreServerManagerImpl();
            if (serverChannel != null) {
                return serverChannel.asBinder();
            }
            return null;
        }

        /**
         * 返回的binder对象会包一层wrapper
         */
        @Override
        public IBinder getService(int serviceId) throws RemoteException {
            ICoreServiceManager service = getCoreServerManagerImpl();
            if (service != null) {
                IBinder binder = service.getService(serviceId);
                if (binder != null) {
                    return RemoteBinderProxy.createInterface(serviceId, binder);
                }
            }
            return null;
        }

        @Override
        public void installOtherManager(IBinder other) throws RemoteException {
            ICoreServiceManager service = getCoreServerManagerImpl();
            if (service != null) {
                service.installOtherManager(other);
            }
        }

        @Override
        public synchronized void binderDied() {
            if (DEBUG) {
                Log.d(TAG, "[binderDied] service channel died, retried.");
            }
            try {
                Thread.sleep((long) ((retriedCount++) / 5.0f * 1000));
            } catch (InterruptedException e) {
            }
            refreshServiceManager();
        }

        private IOtherServiceManager.Stub getOtherServiceManagerImpl() {
            if (null == mOtherServiceManagerImpl) {
                mOtherServiceManagerImpl = new IOtherServiceManager.Stub() {

                    @Override
                    public IBinder getService(int id) throws RemoteException {
                        if (id < ServiceList.MIN_ID || id > ServiceList.MAX_ID) {
                            throw new IllegalArgumentException();
                        }

                        Service serviceCreator = ServiceList.getService(id);
                        if (serviceCreator != null) {
                            return serviceCreator.getService();
                        }else {
                            if (DEBUG) {
                                Log.d(TAG, "[getCoreBundle] serviceCreator == null");
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
     * 传过来的Binder对象的Wrapper，防止binder传进来为空
     *
     * @author chaopei
     */
    private static class RemoteBinderProxy implements IBinder,
            IBinder.DeathRecipient {

        private IBinder mRemote;
        private int mServiceId;

        public static IBinder createInterface(int serviceId, IBinder binder) {

            String descriptor = null;
            try {
                descriptor = binder.getInterfaceDescriptor();
            } catch (RemoteException e) {
            }
            android.os.IInterface iin = binder.queryLocalInterface(descriptor);
            if (((iin != null) && AppUtil.runInServerProcess())) {
                return binder;
            }
            return new RemoteBinderProxy(serviceId, binder);
        }

        private RemoteBinderProxy(int id, IBinder binder) {
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
            remote = CORE_SERVICE_MANAGER_WRAPPER.getOriginalService(mServiceId);
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
    public static IInterface getService(int id) {
        IBinder binder = ServiceList.getCacheBinder(id);
        if (null == binder) {
            if (DEBUG) {
                Log.d(TAG, "[getService]：binder has no cache");
            }
            if (CORE_SERVICE_MANAGER_WRAPPER != null) {
                try {
                    binder = CORE_SERVICE_MANAGER_WRAPPER.getService(id);
                } catch (RemoteException e) {
                    if (DEBUG) {
                        Log.e(TAG, "[getService]：RemoteException", e);
                    }
                }
            }
            if (null != binder) {
                ServiceList.putCacheBinder(id, binder);
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
            Log.d(TAG, "[getService]：binder is not null");
        }
        return ServiceList.getInterface(id, binder);
    }
}
