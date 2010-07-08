package com.sunlightlabs.android.congress.utils;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class LocationUtils {
	public static final String TAG = "CONGRESS";

	public static final int TIMEOUT = 20000; // milliseconds
	public static final int MIN_TIME = 6000; // milliseconds
	public static final int MIN_DIST = 100; // meters

	public interface LocationListenerTimeout extends LocationListener {
		public void onTimeout(String provider);
	}

	private static Message timeoutMsg(String provider) {
		Message msg = new Message();
		msg.obj = provider;
		return msg;
	}

	public static class LocationTimer extends Timer {
		private LocationListenerTimeout listener;
		private LocationManager manager;
		private String provider;
		private Handler handler;

		public LocationTimer(LocationListenerTimeout listener, LocationManager manager,
				String provider, Handler handler) {
			this.listener = listener;
			this.manager = manager;
			this.provider = provider;
			this.handler = handler;
		}

		public void start() {
			if (!manager.isProviderEnabled(provider)) {
				Log.d(TAG, "LocationUtils - start(): provider " + provider + " is not enabled!");
				handler.sendMessage(timeoutMsg(provider));
			} else {
				Log.d(TAG, "LocationUtils - start(): started timer for provider " + provider);
				Timeout task = new Timeout(listener, manager, provider, handler);
				schedule(task, TIMEOUT);
				manager.requestLocationUpdates(provider, MIN_TIME, MIN_DIST, listener);
			}
		}

		@Override
		public void cancel() {
			super.cancel();
			manager.removeUpdates(listener);
			Log.d(TAG, "LocationUtils - cancel(): cancel updating timer and remove listener");
		}
	}

	public static class Timeout extends TimerTask {
		private LocationListenerTimeout listener;
		private LocationManager manager;
		private String provider;
		private Handler handler;

		public void onScreenLoad(LocationListenerTimeout listener) {
			this.listener = listener;
		}

		public Timeout(LocationListenerTimeout listener, LocationManager manager, String provider,
				Handler handler) {
			this.listener = listener;
			this.manager = manager;
			this.provider = provider;
			this.handler = handler;
		}

		@Override
		public void run() {
			Log.d(TAG, "LocationUtils - run(): Timeout! Remove listener from provider " + provider);
			manager.removeUpdates(listener);
			handler.sendMessage(timeoutMsg(provider));
		}
	}

	public static Location getLastKnownLocation(Context context) {
		LocationManager manager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		Location location = null;
		String provider = LocationManager.GPS_PROVIDER;
		if (manager.isProviderEnabled(provider))
			location = manager.getLastKnownLocation(provider);

		if (location == null) {
			provider = LocationManager.NETWORK_PROVIDER;
			if (manager.isProviderEnabled(provider))
				location = manager.getLastKnownLocation(provider);
		}
		return location;
	}

	public static LocationTimer requestLocationUpdate(Context context, Handler handler,
			String provider) {
		Log.d(TAG, "LocationUtils - requestLocationUpdate(): from provider " + provider);

		if (!(context instanceof LocationListener))
			throw new IllegalArgumentException(
					"context must implement LocationListener to receive updates!");

		LocationListenerTimeout listener = (LocationListenerTimeout) context;
		LocationManager manager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		LocationTimer timer = new LocationTimer(listener, manager, provider, handler);
		timer.start();
		return timer;
	}

}
