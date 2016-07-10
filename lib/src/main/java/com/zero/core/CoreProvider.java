package com.zero.core;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

/**
 * Core provider for binder transact.
 */
public class CoreProvider extends ContentProvider {

    private static final boolean DEBUG = AppEnv.DEBUG;

    private static final String TAG = CoreProvider.class.getSimpleName();

    static final String AUTHORITY = "com.zero.core.CoreProvider";

    static final String PATH_SERVICE_PROVIDER = "serviceprovide";

    static final String KEY_SERVICE_MANAGER = PATH_SERVICE_PROVIDER;

    private static final int CODE_SERVICE_PROVIDER = 0;

    @Override
    public boolean onCreate() {
        AppUtil.initCoreProcess(true);
        return false;
    }

    private static final UriMatcher URI_MATCHER;

    private Bundle mCoreBundle;

    private ICoreServiceManager.Stub mCoreImpl;

    private MatrixCursor mCursor;

    static {
        if (DEBUG) {
            Log.d(TAG, "[static init]：running in process " + AppUtil.getProcessName());
        }
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

        URI_MATCHER.addURI(AUTHORITY, PATH_SERVICE_PROVIDER, CODE_SERVICE_PROVIDER);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {

        if (PATH_SERVICE_PROVIDER.equals(method)) {
            if (DEBUG) {
                Log.d(TAG, "[call]：PATH_SERVICE_PROVIDER");
            }
            return getCoreBundle();
        } else {
            return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
		if (DEBUG) {
            Log.d(TAG, "[query]：uri = " + (uri == null ? "null" : uri.toString()));
        }
        final int matchCode = URI_MATCHER.match(uri);

        if (matchCode == CODE_SERVICE_PROVIDER) {
        if (DEBUG) {
                Log.d(TAG, "[query]：CODE_SERVICE_PROVIDER");
            }
            return getCoreCursor();
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }

    private ICoreServiceManager.Stub getCoreImpl() {
        if (null == mCoreImpl) {
            mCoreImpl = new ICoreServiceManager.Stub() {

                @Override
                public IBinder getCoreService(String id) throws RemoteException {
                    if (DEBUG) {
                        Log.d(TAG, "[getCoreBundle] --> serviceId = " + id);
                    }
                    if (!AppUtil.runInCoreProcess()) {
                        if (DEBUG) {
                            Log.d(TAG, "[getCoreBundle] AppUtil not runInCoreProcess");
                        }
                        return null;
                    }

                    if (TextUtils.isEmpty(id)) {
                        throw new IllegalArgumentException();
                    }
                    Service copy = ServiceList.getService(id);
                    if (null == copy) {
                        throw new RuntimeException("Service.install() must be run in every process before getService.");
                    }
                    Service serviceCreator = ServiceList.getService(id);
                    if (serviceCreator != null) {
                        return serviceCreator.getService();
                    } else {
                        if (DEBUG) {
                            Log.d(TAG, "[getCoreBundle] serviceCreator == null");
                        }
                    }
                    return null;
                }

                @Override
                public void installOtherManager(String processName, IBinder other) throws RemoteException {
                    ServiceList.putOtherManager(processName, other);
                }

                @Override
                public IBinder getOtherManager(String processName) throws RemoteException {
                    IBinder manager = ServiceList.getOtherAvailableManager(processName);
                    if (null != manager) {
                        if (DEBUG) {
                            Log.d(TAG, "[getCoreBundle] has found OtherAvailableManager in process " + processName);
                        }
                        return manager;
                    }
                    if (DEBUG) {
                        Log.d(TAG, "[getCoreBundle] no OtherAvailableManager found in process " + processName);
                    }
                    return null;
                }
            };
        }
        return mCoreImpl;
    }

    private Bundle getCoreBundle() {
        if (null == mCoreBundle) {
            ICoreServiceManager.Stub coreImpl = getCoreImpl();

            mCoreBundle = new Bundle();

            mCoreBundle.putParcelable(CoreProvider.KEY_SERVICE_MANAGER, new ServiceParcel(coreImpl));
        }
        return mCoreBundle;
    }

    private Cursor getCoreCursor() {
        if (null == mCursor) {
            /**
             * 返回给客户端的MatrixCursor
             */
            mCursor = new MatrixCursor(new String[] { "s" }) {
                @Override
                public Bundle getExtras() {
                    return getCoreBundle();
                }
            };
        }
        return mCursor;
    }
}
