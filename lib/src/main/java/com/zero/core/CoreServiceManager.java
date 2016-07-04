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
     * 进程单例
     */
    private static ServiceManagerWrapper sServiceManagerWrapper = new ServiceManagerWrapper();

    static ServiceManagerWrapper getServiceManagerImpl() {
        return sServiceManagerWrapper;
    }

    private static class ServiceManagerWrapper implements
            ICoreServiceManager, IBinder.DeathRecipient {

        private ICoreServiceManager mServerManagerImpl;

        private volatile int retriedCount = 0;

        synchronized private ICoreServiceManager getCoreServerManagerImpl() {
            if (null == mServerManagerImpl) {
                refreshServiceManager();
            }
            return mServerManagerImpl;
        }

        private void refreshServiceManager() {
            mServerManagerImpl = fetchLocked();
            if (null != mServerManagerImpl) {
                //todo 初始化 other managers
                try {
                    mServerManagerImpl.asBinder().linkToDeath(this, 0);
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
        IBinder getOriginalService(int serviceId) throws RemoteException {
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
                    return RemoteBinderWrapper.getService(serviceId, binder);
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
                Thread.sleep((long)((retriedCount++) / 5.0f * 1000));
            } catch (InterruptedException e) {
            }
            refreshServiceManager();
        }
    }

    /**
     * 传过来的Binder对象的Wrapper，防止binder传进来为空
     *
     * @author chaopei
     */
    private static class RemoteBinderWrapper implements IBinder,
            IBinder.DeathRecipient {

        private IBinder mRemoteBinderImpl;
        private int mServiceId;

        public static IBinder getService(int serviceId, IBinder binder) {

            String descriptor = null;
            try {
                descriptor = binder.getInterfaceDescriptor();
            } catch (RemoteException e) {
            }
            android.os.IInterface iin = binder.queryLocalInterface(descriptor);
            if (((iin != null) && AppUtil.runInServerProcess())) {
                return binder;
            }
            return new RemoteBinderWrapper(serviceId, binder);
        }

        private RemoteBinderWrapper(int id, IBinder binder) {
            mRemoteBinderImpl = binder;
            mServiceId = id;
            try {
                mRemoteBinderImpl.linkToDeath(this, 0);
            } catch (RemoteException e) {
                if (DEBUG) {
                    Log.e(TAG,
                            "[ModuleChannelWrapper constructor]：RemoteException",
                            e);
                }
            }
        }

        private IBinder getRemoteBinder() throws RemoteException {
            IBinder remote = mRemoteBinderImpl;
            if (remote != null) {
                return remote;
            }
            ServiceManagerWrapper serverChannel = (ServiceManagerWrapper) getServiceManagerImpl();
            remote = serverChannel.getOriginalService(mServiceId);
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
            mRemoteBinderImpl = null;
            ServiceList.removeCacheBinder(mServiceId);
        }

        public void dumpAsync(FileDescriptor fd, String[] args)
                throws RemoteException {
            if (DEBUG) {
                Log.d(TAG, "[dumpAsync]");
            }
        }

    }

    /**
     * 拿到IBinder后直接转接口，外部一律用此接口
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
            ServiceManagerWrapper serviceManagerWrapper = getServiceManagerImpl();
            if (serviceManagerWrapper != null) {
                try {
                    binder = serviceManagerWrapper.getService(id);
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
