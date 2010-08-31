package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.content.Context;
import android.content.Intent;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;

public abstract class NotificationFinder {
	public Context context;
	
	public NotificationFinder() {}

	// must return results in order of most recent first
	public abstract List<?> fetchUpdates(Subscription subscription);
	
	// must be prepared to receive an object of the same type that's in the List<?> returned by fetchUpdates 
	public abstract String decodeId(Object result);
	
	public abstract Intent notificationIntent(Subscription subscription);

	public int notificationId(Subscription subscription) {
		return (subscription.id + subscription.notificationClass).hashCode();
	}

	
	// can be overridden by subclasses
	public String notificationMessage(Subscription subscription, int results) {
		return Utils.formatStringResource(context.getString(R.string.notification_message), 
				results, subscription.notificationName(), subscription.name );
	}

	public String notificationTitle(Subscription subscription) {
		return Utils.capitalize(Utils.formatStringResource(
				context.getString(R.string.notification_title), 
				subscription.notificationName(), 
				subscription.name));
	}
}
