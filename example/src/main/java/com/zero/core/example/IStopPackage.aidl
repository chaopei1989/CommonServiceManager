package com.zero.core.example;

import android.view.Surface;

interface IStopPackage {

	oneway void killSysNoWait();
	
	void killSysWait();
	
}