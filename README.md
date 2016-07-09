# 这是什么

这是一个使用方便、可扩展的服务接口管理库。
Github 地址：[https://github.com/lollipopgood/CommonServiceManager][1]

# 为什么用它
当工程规模从小变大，拆分进程就是有效降低单内存消耗提高进程存活率的首选办法，更能够将工程解耦，变得有扩展性，但与此同时带来了跨进程通信的不便。

为了支持跨进程通信，通常会在不同的进程空间创建服务（Service），好让其他进程去 bindService 拿到接口，但是这个接口又是个异步接口，当我们想用 Server 进程的某个接口时在当前线程使用 bindService，但并不能立即拿到接口去执行，需要等待 onServiceConnected （在另一个线程）回调，这样一来增加了代码复杂度，也增加了数据同步的不可靠性，而在 CommonServiceManager 中我们采用同步的方式获取接口来优化，降低编程复杂度，提高数据可靠性。

下图对比展示了 bindService（左）和 CommonServiceManager（右）中的调用流程。

![commonserviceManager.png][2]

另外 commonserviceManager 还有服务可扩展、进程可扩展的特性。

# 如何使用

 - 下载源码后引入工程代码
 - 将 CoreProvider 引入自己工程的 AndroidManifest.xml
 
```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.zero.core.example">

    <application
        android:name=".MyApplication">

        <provider
            android:name="com.zero.core.CoreProvider"
            android:authorities="com.zero.core.CoreProvider"
            android:exported="false"
            android:process=":server" >
        </provider>
    </application>

</manifest>
```
注意该 provider 最好不要导出。
 - 创建服务接口：建立自定义接口的 ADIL 文件（服务接口必须是 AIDL 的声明），如 example 模块中
 
```
package com.zero.core.example;
import android.view.Surface;
interface IStopPackage {
	oneway void killSysNoWait();
	void killSysWait();
}
```

 - 创建服务说明（Service），如 example 模块中
 
```
public class StopPackageService extends IStopPackage.Stub {
    public static final String SERVICE_ID = "StopPackageService";

    public static final Service INSTALLER = new Service(StopPackageService.class) {
        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }
    };

    @Override
    public void killSysNoWait() throws RemoteException {
        //todo
    }

    @Override
    public void killSysWait() throws RemoteException {
        //todo
    }

}
```

StopPackageService 是在 core 进程实现的一个服务接口，INSTALLER 是声明的服务说明，建议每个服务接口类都这么写。
 - 在自定义的 Application 中尽早地初始化每个服务接口和 AppUtil，如 example 模块中
 
```
public class MyApplication extends Application {

    static {
        if (DEBUG) {
            Log.d(TAG, "[static init]：running in process " + AppUtil.getProcessName());
        }
        StopPackageService.INSTALLER.install();
        StopPackageUI.INSTALLER.install();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppUtil.init(this); // needed!
    }

}
```

使用 INSTALLER.install 方法在每个进程安装该服务说明，就像是给每个人发了一份说明书，有了这个说明书才知道用什么服务。使用 AppUtil.init 初始化 Context。
 - 使用调用服务：CoreServiceManager.getService(...)，如 example 模块中
 
```
IStopPackage i = (IStopPackage) CoreServiceManager.getService(StopPackageService.SERVICE_ID);
try {
    i.killSysNoWait();
} catch (RemoteException e) {
}
```

# 注意事项

 - 不可自行修改进程名；
 - 一定要调用 AppUitl.init 方法，将 Application 传入；
 - ServiceId 最好是纯英文无空格无符号，不可重复；
 - getProcessSuffix 返回的进程后缀严格遵守这三种情形：
    1. core 进程返回 null，此情形不用覆写
    2. 主(ui)进程返回空字符串（Service.PROCESS_MAIN_SUFFIX）
    3. 其他进程返回的字符串以 ":" 开头

 


  [1]: https://github.com/lollipopgood/CommonServiceManager
  [2]: http://www.3dobe.com/usr/uploads/2016/07/7646042.png
