/***
	Copyright (c) 2009 CommonsWare, LLC
	
	Licensed under the Apache License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may obtain
	a copy of the License at
		http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */

package com.commonsware.cwac.wakeful;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

abstract public class WakefulIntentService extends IntentService {
	abstract protected void doWakefulWork(Intent intent);

	public static final String LOCK_NAME_STATIC = "com.commonsware.cwac.wakeful.WakefulIntentService";
	private static PowerManager.WakeLock lockCpu = null;
	private static WifiManager.WifiLock lockWifi = null;

	public static void acquireStaticLock(Context context) {
		if (!getCpuLock(context).isHeld())
			getCpuLock(context).acquire();

		WifiManager.WifiLock lock = getWifiLock(context);
		try {
			if (!lock.isHeld())
				lock.acquire();
		}
		// too many wifi locks, couldn't acquire one
		catch (UnsupportedOperationException ex) {
			// swallow it. oh well, no wifi lock this time.
		}
	}

	synchronized protected static PowerManager.WakeLock getCpuLock(Context context) {
		if (lockCpu == null) {
			PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

			// wake up the CPU
			lockCpu = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,LOCK_NAME_STATIC);
			lockCpu.setReferenceCounted(true);
		}

		return lockCpu;
	}

	synchronized protected static WifiManager.WifiLock getWifiLock(Context context) {
		if (lockWifi == null) {
			WifiManager mgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

			// wake up the WiFi
			lockWifi = mgr.createWifiLock(LOCK_NAME_STATIC);
			lockWifi.setReferenceCounted(true);
		}

		return lockWifi;
	}

	public static void sendWakefulWork(Context ctxt, Intent i) {
		acquireStaticLock(ctxt);
		ctxt.startService(i);
	}

	public static void sendWakefulWork(Context ctxt, Class<?> clsService) {
		sendWakefulWork(ctxt, new Intent(ctxt, clsService));
	}

	public WakefulIntentService(String name) {
		super(name);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (!getCpuLock(this).isHeld()) // fail-safe for crash restart
			getCpuLock(this).acquire();
		
		if (!getWifiLock(this).isHeld()) 
			getWifiLock(this).acquire();
		
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		// must release locks when the service is destroyed, otherwise will drain the battery
		if (getCpuLock(this).isHeld())
			getCpuLock(this).release();
		
		if (getWifiLock(this).isHeld())
			getWifiLock(this).release();
		
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			doWakefulWork(intent);
		} finally {
			if (getCpuLock(this).isHeld())
				getCpuLock(this).release();
			if (getWifiLock(this).isHeld())
				getWifiLock(this).release();
		}
	}
}