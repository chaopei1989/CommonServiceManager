package com.zero.core;

interface ICoreServiceManager {

	IBinder getCoreService(String id);

	void installOtherManager(String callingProcessName, IBinder other);

	IBinder getOtherManager(String processName);

}