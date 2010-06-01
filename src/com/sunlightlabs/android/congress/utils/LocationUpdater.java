package com.sunlightlabs.android.congress.utils;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.sunlightlabs.congress.java.CongressException;

public class LocationUpdater implements LocationListener {
	private static final long TIMEOUT = 10000; // 10 seconds

	private String provider;
	private LocationManager manager;
	private LocationUpdateable<? extends Context> context;

	private boolean processing;
	private Timer timeoutTimer;

	public interface LocationUpdateable<C extends Context> {
		void onLocationUpdate(Location location);
		void onLocationUpdateError(CongressException e);
	}

	public LocationUpdater(LocationUpdateable<? extends Context> context) {
		this.context = context;
		init();
	}

	private void init() {
		processing = false;
		manager = (LocationManager) ((Context)context).getSystemService(Context.LOCATION_SERVICE);
		provider = getProvider();
	}

	public void onScreenLoad(LocationUpdateable<? extends Context> context) {
		this.context = context;
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

	public void requestLocationUpdate() {
		if(provider != null) {
			processing = true;
			prepareTimeout();
			manager.requestLocationUpdates(provider, 0, 0, this);
		}
		else
			context.onLocationUpdateError(new CongressException("Cannot update the current location. All providers are disabled."));
	}

	private void prepareTimeout() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Log.v("===LocationUpdater===", "TIMEOUT!");
				context.onLocationUpdateError(new CongressException("Cannot update the current location."));
			}
		};
		cancelTimeout();
		timeoutTimer = new Timer();
		timeoutTimer.schedule(task, TIMEOUT);
	}

	private void cancelTimeout() {
		if (timeoutTimer != null) {
			timeoutTimer.cancel();
			timeoutTimer = null;
		}
	}

	public void onLocationChanged(Location location) {
		cancelTimeout();
		context.onLocationUpdate(location);
		manager.removeUpdates(this);
		processing = false;
	}

	public void onProviderDisabled(String provider) {
		if(processing) { // currently is processing a request and the provider gets disabled
			provider = getProvider(); // check for other enabled providers
			if(provider == null) {
				cancelTimeout();
				context.onLocationUpdateError(new CongressException("Cannot update the current location. All providers are disabled."));
				processing = false;
			}
		}
	}

	public void onProviderEnabled(String provider) {}
	public void onStatusChanged(String provider, int status, Bundle extras) {}
}
