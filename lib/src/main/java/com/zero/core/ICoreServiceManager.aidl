package com.zero.core;

interface ICoreServiceManager {

	IBinder getService(int id);

	void installOtherManager(IBinder other);

}