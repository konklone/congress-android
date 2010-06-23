package com.sunlightlabs.android.congress.utils;

import java.util.ArrayList;
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

public class LocationUpdater {
	private static final String TAG = "CONGRESS";
	private static final long TIMEOUT = 20000;
	private static final long MIN_TIME = 6000; // 6 seconds between updates
	private static final int MSG_TIMEOUT = 100;

	private String provider;
	private LocationManager manager;
	private LocationUpdateable<? extends Context> context;
	private LocationListener listener;
	private boolean processing = false;

	private ArrayList<String> triedProviders;
	private ArrayList<String> availableProviders;

	private Timer timeout;

	public interface LocationUpdateable<C extends Context> {
		Handler getHandler();
		void onLocationUpdate(Location location);
		void onLocationUpdateError(CongressException e);
	}

	public LocationUpdater(LocationUpdateable<? extends Context> context) {
		this.context = context;
		this.listener = (LocationListener) context;
		init();
	}

	private void init() {
		manager = (LocationManager) ((Context) context).getSystemService(Context.LOCATION_SERVICE);
		triedProviders = new ArrayList<String>();
		availableProviders = new ArrayList<String>();
		getAvailableProviders();
		getProvider();
	}

	public void onScreenLoad(LocationUpdateable<? extends Context> context) {
		this.context = context;
		this.listener = (LocationListener) context;
		Log.d(TAG, "onScreenLoad(): context changed to " + context);
	}

	private void getAvailableProviders() {
		if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			availableProviders.add(LocationManager.GPS_PROVIDER);
		if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			availableProviders.add(LocationManager.NETWORK_PROVIDER);
	}

	private void getProvider() {
		if (availableProviders.size() > 0) {
			provider = availableProviders.get(0);
			triedProviders.add(provider);
		}
	}

	private String switchProvider(String lastProvider) {
		getAvailableProviders();
		for (int i = 0; i < availableProviders.size(); i++) {
			String prov = availableProviders.get(i);
			if (!triedProviders.contains(prov)) {
				triedProviders.add(prov);
				return prov;
			}
		}
		return lastProvider;
	}

	public Location getLastKnownLocation() {
		// check the last known location for all the available providers
		for(int i = 0; i < availableProviders.size(); i++) {
			Location loc = manager.getLastKnownLocation(availableProviders.get(i));
			if (loc != null) {
				Log.d(TAG, "Last known location is " + loc);
				return loc;
			}
		}
		return null;
	}

	private void prepareTimeout() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				String newProvider = switchProvider(provider);
				// try another provider before quitting
				if (!provider.equals(newProvider)) {
					provider = newProvider;
					Log.d(TAG, "New provider is " + provider + ". Requesting a new update.");
					requestLocationUpdate();
				}
				else {
					Message msg = new Message();
					msg.arg1 = MSG_TIMEOUT;
					msg.obj = new CongressException("Could not update location. Timeout.");
					Log.d(TAG, "prepareTimeout(): sending message=" + msg);
					context.getHandler().sendMessage(msg);
					processing = false;
				}
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
			processing = false;
		}
	}

	public void requestLocationUpdate() {
		if (provider != null) {
			processing = true;
			prepareTimeout();
			manager.requestLocationUpdates(provider, MIN_TIME, 0, listener);
			Log.d(TAG, "requestLocationUpdate(): provider=" + provider);
		} else {
			context.onLocationUpdateError(new CongressException(
					"Cannot update the current location. All providers are disabled."));
			Log.d(TAG, "requestLocationUpdate(): provider=null");
		}
	}

	public void onLocationChanged(Location location) {
		Log.d(TAG, "onLocationChanged(): location=" + location + "; thread="
				+ Thread.currentThread().getName());
		context.onLocationUpdate(location);
		processing = false;
		cancelTimeout();
	}

	public void onProviderDisabled(String provider) {
		if (processing) { // currently is processing a request and the provider gets disabled
			getProvider(); // check for other enabled providers
			if (provider == null) {
				Log.d(TAG, "onProviderDisabled(): provider=null; thread="
						+ Thread.currentThread().getName());
				context.onLocationUpdateError(new CongressException(
						"Cannot update the current location. All providers are disabled."));
				processing = false;
				cancelTimeout();
			}
		}
	}

	public void requestLocationUpdateHalt() {
		Log.d(TAG, "Removing update listener " + listener);
		cancelTimeout();
		if (manager != null)
			manager.removeUpdates(listener);
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public boolean getProcessing() {
		return processing;
	}
}
