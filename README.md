# 这是什么

CommonServiceManager 是一个使用方便、可扩展的服务接口管理库。
Github 地址：[https://github.com/lollipopgood/CommonServiceManager][1]

<!--more-->

# 项目背景
当工程规模从小变大，拆分进程就是有效降低单内存消耗提高进程存活率的首选办法，更能够将工程解耦，变得有扩展性，但与此同时带来了跨进程通信的不便。

为了支持跨进程通信，通常会在不同的进程空间创建服务（Service），好让其他进程去 bindService 拿到接口，但是这个接口又是个异步接口，当我们想用 Server 进程的某个接口时在当前线程使用 bindService，但并不能立即拿到接口去执行，需要等待 onServiceConnected （在另一个线程）回调，这样一来增加了代码复杂度，也增加了数据同步的不可靠性，而在 CommonServiceManager 中我们采用同步的方式获取接口来优化，降低编程复杂度，提高数据可靠性。



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
 - core 进程是常驻性质的进程，其中的 CoreProvider 起到一个中心管理的作用，即使 core 进程被主动杀死，也会被其他进程通过查询 Provider 带起来，而其他进程则不会被自动带起来，所以使用时请注意，当服务接口在其他进程时，如果目标进程不存在则调用失败，但当服务接口在 core 进程时则不会有这个问题（如果你想自动带起非 core 进程，请想别的办法）。

# 与 bindService 相比

### bindService 的弊端
例如有一个需求 “某个界面中检测在有软件安装时查询该软件是否在白名单中”，程序有主进程和 server 进程，在 server 进程中由于高频使用白名单，白名单读写和缓存都在 server 进程，那么主进程想查询则需要跨进程去查。

如果在 server 进程中实现一个 Service，使用 bindService 的基本流程如下述代码所示：

```
public class MainActivity extends Activity {

    private IWhiteListImpl mWhiteListImpl;

    private BroadcastReceiver mBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
                if (null != mWhiteListImpl) { // 不知道 mWhiteListImpl 什么时候拿得到，需要判断是否为空
                    String packageName = intent.getData().getSchemeSpecificPart();
                    if (mWhiteListImpl.isWhite(packageName)) {
                        //todo
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ... // ui init
        registerReceiver(mBr, new IntentFilter(Intent.ACTION_PACKAGE_ADDED));
        bindService(new Intent(this, WhiteListService.class), new ServiceConnection() { // 使用 bindService
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mWhiteListImpl = IWhiteListImpl.Stub.asInterface(service); // 异步回调
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mWhiteListImpl = null;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBr);
    }
}
```

bindService 异步获取接口的弊病就体现出来了，那就是 bindService 后不知道什么时候能够回调，导致代码结构比较冗余复杂，例如每次都需要考虑 mWhiteListImpl 还未回调（还须等会儿）、失败后重新绑定、断连等情况，又例如每次调用都有一个 ServiceConnection 对象（当然这个问题可以用全局的绑定来解决），但是这并不符合一个 Service 设计的初衷。

### CommonServiceManager 的做法
如果采用本库的做法，则代码可以简洁如下：

```
public class MainActivity extends Activity {

    private BroadcastReceiver mBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
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
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ... // ui init
        registerReceiver(mBr, new IntentFilter(Intent.ACTION_PACKAGE_ADDED));
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBr);
    }
}
```

会发现除了必须的注册、注销广播接收器外，onCreate 中就没有其他的逻辑了，服务接口就等到使用的时候再去取就是了，代码可读性好、函数功能清晰。

下图对比展示了 bindService（左）和 CommonServiceManager（右）中的调用流程。

![commonserviceManager.png][2]

另外 CommonserviceManager 还有服务可扩展、进程可扩展的特性。
# 核心结构

![CommonServiceManager_process.png][3]


  [1]: https://github.com/lollipopgood/CommonServiceManager
  [2]: http://www.3dobe.com/usr/uploads/2016/07/7646042.png
  [3]: http://www.3dobe.com/usr/uploads/2016/07/2118837698.png
  
