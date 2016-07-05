package com.zero.core;

interface ICoreServiceManager {

	IBinder getService(int id);

	void installOtherManager(String callingProcessName, IBinder other);

}