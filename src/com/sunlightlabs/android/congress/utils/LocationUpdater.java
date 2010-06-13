package com.sunlightlabs.android.congress.utils;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.sunlightlabs.congress.models.CongressException;

public class LocationUpdater implements LocationListener {
	private static final String TAG = "CONGRESS";
	private static final long TIMEOUT = 20000;
	private static final int MSG_TIMEOUT = 100;

	private String provider;
	private LocationManager manager;
	private LocationUpdateable<? extends Context> context;
	private boolean processing = false;

	private Timer timeout;

	public interface LocationUpdateable<C extends Context> {
		Handler getHandler();
		void onLocationUpdate(Location location);
		void onLocationUpdateError(CongressException e);
	}

	public LocationUpdater(LocationUpdateable<? extends Context> context) {
		this.context = context;
		init();
	}

	private void init() {
		manager = (LocationManager) ((Context)context).getSystemService(Context.LOCATION_SERVICE);
		provider = getProvider();
	}

	public void onScreenLoad(LocationUpdateable<? extends Context> context) {
		this.context = context;
		Log.d(TAG, "onScreenLoad(): context changed to " + context);
	}

	private String getProvider() {
		String provider = null;
		if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			provider = LocationManager.GPS_PROVIDER;
		else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			provider = LocationManager.NETWORK_PROVIDER;
		return provider;
	}

	public Location getLastKnownLocation() {
		if (provider != null)
			return manager.getLastKnownLocation(provider);
		return null;
	}

	private void prepareTimeout() {
		TimerTask task = new TimerTask() {			
			@Override
			public void run() {
				Message msg = new Message();
				msg.arg1 = MSG_TIMEOUT;
				msg.obj = new CongressException("Could not update location. Timeout.");
				Log.d(TAG, "prepareTimeout(): sending message=" + msg);
				context.getHandler().sendMessage(msg);
			}
		};
		cancelTimeout();
		timeout = new Timer();
		timeout.schedule(task, TIMEOUT);
	}

	private void cancelTimeout() {
		if (timeout != null) {
			Log.d(TAG, "cancelTimeout(): canceling");
			timeout.cancel();
			timeout = null;
		}
	}

	public void requestLocationUpdate() {
		if(provider != null) {
			processing = true;
			prepareTimeout();
			manager.requestLocationUpdates(provider, 0, 0, this);
			Log.d(TAG, "requestLocationUpdate(): provoder=" + provider);
		}
		else {
			context.onLocationUpdateError(new CongressException("Cannot update the current location. All providers are disabled."));
			Log.d(TAG, "requestLocationUpdate(): provider=null");
		}
	}

	public void onLocationChanged(Location location) {
		Log.d(TAG, "onLocationChanged(): location=" + location + "; thread=" + Thread.currentThread().getName());
		context.onLocationUpdate(location);
		manager.removeUpdates(this);
		processing = false;
		cancelTimeout();
	}

	public void onProviderDisabled(String provider) {		
		if(processing) { // currently is processing a request and the provider gets disabled
			provider = getProvider(); // check for other enabled providers
			if(provider == null) {
				Log.d(TAG, "onProviderDisabled(): provider=null; thread=" + Thread.currentThread().getName());
				context.onLocationUpdateError(new CongressException("Cannot update the current location. All providers are disabled."));
				processing = false;
				cancelTimeout();
			}
		}
	}

	public void onProviderEnabled(String provider) {}
	public void onStatusChanged(String provider, int status, Bundle extras) {}
}
