package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.content.Context;
import android.content.Intent;

import com.sunlightlabs.congress.models.Bill;

public abstract class NotificationFinder {
	public Context context;
	
	public NotificationFinder() {}

	// must return results in order of most recent first
	public abstract List<?> fetchUpdates(Subscription subscription);
	
	// must be prepared to receive an object of the same type that's in the List<?> returned by fetchUpdates 
	public abstract String decodeId(Object result);
	
	
	// notification formatting methods
	
	public String notificationTicker(Subscription subscription) {
		return "Updates for " + subscription.name;
	}
	public String notificationTitle(Subscription subscription) {
		return subscription.name;
	}
	public abstract String notificationMessage(Subscription subscription, int results);
	public abstract Intent notificationIntent(Subscription subscription);
	
	
	// utility methods
	
	public static String notificationName(Bill bill) {
		return Bill.formatCode(bill.code);
	}
}