package com.sunlightlabs.android.congress.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.sunlightlabs.congress.java.CongressException;

public class LocationUpdater implements LocationListener {
	private String provider;
	private LocationManager manager;
	private LocationUpdateable<? extends Context> context;
	private boolean processing;

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
			manager.requestLocationUpdates(provider, 0, 0, this);
		}
		else
			context.onLocationUpdateError(new CongressException("Cannot update the current location. All providers are disabled."));
	}

	public void onLocationChanged(Location location) {
		context.onLocationUpdate(location);
		manager.removeUpdates(this);
		processing = false;
	}

	public void onProviderDisabled(String provider) {
		if(processing) { // currently is processing a request and the provider gets disabled
			provider = getProvider(); // check for other enabled providers
			if(provider == null) {
				context.onLocationUpdateError(new CongressException("Cannot update the current location. All providers are disabled."));
				processing = false;
			}
		}
	}

	public void onProviderEnabled(String provider) {}
	public void onStatusChanged(String provider, int status, Bundle extras) {}
}
