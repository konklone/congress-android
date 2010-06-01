package com.sunlightlabs.android.congress.utils;

import java.util.Iterator;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;

import com.sunlightlabs.congress.java.CongressException;

public class LocationUpdater implements LocationListener {
	private String provider;
	private LocationManager manager;
	private ConnectivityManager connectivityManager;
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
		connectivityManager = (ConnectivityManager) ((Context)context).getSystemService(Context.CONNECTIVITY_SERVICE);
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

	private boolean isWiFiEnabled() {
		return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
	}

	private boolean isGpsEnabled() {
		Iterator<GpsSatellite> satellites = manager.getGpsStatus(null).getSatellites().iterator();
		return (satellites == null) ? false : satellites.hasNext();
	}

	public void requestLocationUpdate() {
		if(provider != null) {
			// check to see if the connectivity is enabled
			if(provider == LocationManager.NETWORK_PROVIDER) {
				if(!isWiFiEnabled()) {
					context.onLocationUpdateError(new CongressException("Cannot update the current location. Wi-fi is disabled."));
					return;
				}
			}
			else if(provider == LocationManager.GPS_PROVIDER) {
				if(!isGpsEnabled()) {
					context.onLocationUpdateError(new CongressException("Cannot update the current location. Gps is disabled."));
					return;
				}
			}
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
