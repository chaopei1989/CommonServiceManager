package com.zero.core.example;

import android.view.Surface;

interface IStopPackageService {

	oneway void killNoWait(in String name);
	
	void killWait(in String name);
	
	oneway void killAllNoWait(in String[] names);
	
	void killAllWait(in String[] names);
	
	oneway void killSysNoWait();
	
	void killSysWait();
	
	oneway void killUserNoWait();
	
	void killUserWait(inout Surface a);
	
}