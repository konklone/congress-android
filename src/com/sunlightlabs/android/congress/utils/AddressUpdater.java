package com.sunlightlabs.android.congress.utils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

import com.sunlightlabs.congress.java.CongressException;

public class AddressUpdater extends AsyncTask<Location, Void, String> {
	// cache the addresses obtained for different locations
	private static final int MAX_CACHE_SIZE = 10;
	private static LinkedHashMap<String, String> cache = new LinkedHashMap<String, String>();	

	private AddressUpdateable<? extends Context> context;

	public interface AddressUpdateable<C extends Context> {
		void onAddressUpdate(String address);
		void onAddressUpdateError(CongressException e);
	}

	// Location doesn't override equals() or hashCode(), so we use latitude and longitude as key
	public static void addToCache(Location location, String area) {
		if(cache.size() > 0 && cache.size() == MAX_CACHE_SIZE)
			cache.remove(cache.keySet().iterator().next());
		cache.put(location.getLatitude() + "-" + location.getLongitude(), area);
	}

	public static String getFromCache(Location location) {
		return cache.get(location.getLatitude() + "-" + location.getLongitude());
	}

	public static String getArea(Address address) {
		String area = address.getAdminArea();
		area = (area == null) ? address.getLocality() : area;
		area = (area == null) ? "" : area;
		return area;
	}

	public AddressUpdater(AddressUpdateable<? extends Context> context) {
		this.context = context;
	}

	public void onScreenLoad(AddressUpdateable<? extends Context> context) {
		this.context = context;
	}

	@Override
	protected String doInBackground(Location... params) {
		if(params == null || params.length == 0) 
			return null;

		try {
			Location location = params[0];	
			String address = "";

			List<Address> addresses = new Geocoder((Context) context).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if (addresses != null && addresses.size() > 0) {
				address = getArea(addresses.get(0));
				addToCache(location, address); 
				return address;
			}
		} catch (IOException e) {}

		return null;
	}

	@Override
	protected void onPostExecute(String address) {
		if(address != null)
			context.onAddressUpdate(address);
		else
			context.onAddressUpdateError(new CongressException("Cannot update the current address."));
	}
}
