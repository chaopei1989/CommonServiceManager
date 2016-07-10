package com.zero.core.example;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.os.Bundle;

import com.zero.core.CoreServiceManager;

public class MainActivity extends Activity {

    private BroadcastReceiver mBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getData().getSchemeSpecificPart();
            try {
                IWhiteListImpl impl = (IWhiteListImpl) CoreServiceManager.getService(WhiteListService.SERVICE_ID); // 现拿现用
                if (null != impl && impl.isWhite(packageName)) {
                    // todo
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(mBr, new IntentFilter(Intent.ACTION_PACKAGE_ADDED));
//        IStopPackage i = (IStopPackage) CoreServiceManager.getService(StopPackageService.SERVICE_ID);
//        try {
//            i.killSysNoWait();
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBr);
    }
}
