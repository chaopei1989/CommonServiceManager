package com.zero.core.example;

import android.app.Activity;
import android.os.RemoteException;
import android.os.Bundle;

import com.zero.core.CommonServiceManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IStopPackageService i = (IStopPackageService) CommonServiceManager.getService(StopPackageService.SERVICE_ID);
        try {
            i.killSysNoWait();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
