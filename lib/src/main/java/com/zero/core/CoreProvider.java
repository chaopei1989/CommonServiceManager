package com.zero.core;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class CoreProvider extends ContentProvider {
	
	private static final boolean DEBUG = AppEnv.DEBUG;
	
	private static final String TAG = CoreProvider.class.getSimpleName();
	
	static final String AUTHORITY = "com.zero.core.CoreProvider";
	
	static final String PATH_SERVICE_PROVIDER = "serviceprovide";
    
    private static final int CODE_SERVICE_PROVIDER = 0;
    
	@Override
	public boolean onCreate() {
		AppUtil.initServerProcess(true);
		return false;
	}
	
	private static final UriMatcher URI_MATCHER;

    private Bundle mCoreBundle;

//    private static MatrixCursor sCursor;

    static {
        if (DEBUG) {
            Log.d(TAG, "[static init]：running in process " + AppUtil.getProcessName());
        }
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

        URI_MATCHER.addURI(AUTHORITY, PATH_SERVICE_PROVIDER, CODE_SERVICE_PROVIDER);

//        /**
//         * 返回给客户端的MatrixCursor
//         */
//        sCursor = new MatrixCursor(new String[] { "s" }) {
//
//            @Override
//            public Bundle getExtras() {
//                Bundle extra = new Bundle();
//                extra.putParcelable(CoreServiceManager.SERVICE_MANAGER_KEY, new ServiceParcel(sCoreServiceManagerImpl));
//                return extra;
//            }
//        };
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
//		if (DEBUG) {
//            Log.d(TAG, "[query]：uri = " + (uri == null ? "null" : uri.toString()));
//        }
//        final int matchCode = URI_MATCHER.match(uri);
//
//        if (matchCode == CODE_SERVICE_PROVIDER) {
//        	if (DEBUG) {
//                Log.d(TAG, "[query]：CODE_SERVICE_PROVIDER");
//            }
//            return sCursor;
//        }
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

    private Bundle getCoreBundle() {
        if (null == mCoreBundle) {
            ICoreServiceManager.Stub coreServiceManagerImpl = new ICoreServiceManager.Stub() {

                @Override
                public IBinder getService(int id) throws RemoteException {
                    if (DEBUG) {
                        Log.d(TAG, "[getService] --> serviceId = " + id);
                    }
                    if (!AppUtil.runInServerProcess()) {
                        if (DEBUG) {
                            Log.d(TAG, "[getService] AppUtil not runInServerProcess");
                        }
                        return null;
                    }

                    if (id < ServiceList.MIN_ID || id > ServiceList.MAX_ID) {
                        throw new IllegalArgumentException();
                    }

                    Service serviceCreator = ServiceList.getService(id);
                    if (serviceCreator != null) {
                        return serviceCreator.getService();
                    }else {
                        if (DEBUG) {
                            Log.d(TAG, "[getService] serviceCreator == null");
                        }
                    }

                    return null;
                }

                @Override
                public void installOtherManager(IBinder other) throws RemoteException {
                    // todo
                }
            };

            mCoreBundle = new Bundle();

            mCoreBundle.putParcelable(CoreServiceManager.SERVICE_MANAGER_KEY, new ServiceParcel(coreServiceManagerImpl));
        }
        return mCoreBundle;
    }
}
