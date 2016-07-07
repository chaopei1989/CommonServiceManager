# 这是什么

这是一个使用方便、可扩展的服务接口管理库。


# 为什么用它
当工程规模从小变大，拆分进程就是有效降低单内存消耗提高进程存活率的首选办法，更能够将工程解耦，变得有扩展性，但与此同时带来了跨进程通信的不便。

为了支持跨进程通信，通常会在不同的进程空间创建服务（Service），好让其他进程去 bindService 拿到接口，但是这个接口又是个异步接口，当我们想用 Server 进程的某个接口时在当前线程使用 bindService，但并不能立即拿到接口继续执行其他代码，需要等待 onServiceConnected （在另一个线程）回调，这样一来增加了代码复杂度，也增加了数据同步的不可靠性，而在 CommonServiceManager 中我们采用同步的方式获取接口，降低编程复杂度和数据可靠性。

下图对比展示了 bindService（左）和 CommonServiceManager（右）中的调用流程。

![commonserviceManager.png][1]

另外 commonserviceManager 还有服务可扩展、进程可扩展的特性。

# 如何使用

 - 下载源码后引入工程代码

# 注意事项

 - 不可自行修改进程名；
 - 一定要调用 AppUitl.init 方法，将 Application 传入；
 - ServiceId 最好是纯英文无空格无符号，不可重复；
 - getProcessSuffix 返回的进程后缀严格遵守这三种情形：
    1. core 进程返回 null，此情形不用覆写
    2. 主(ui)进程返回空字符串（Service.PROCESS_MAIN_SUFFIX）
    3. 其他进程返回的字符串以 ":" 开头

 

  [1]: http://www.3dobe.com/usr/uploads/2016/07/7646042.png