package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.content.Context;
import android.content.Intent;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;

public abstract class NotificationFinder {
	public Context context;
	
	public NotificationFinder() {}

	public abstract String decodeId(Object result);
	
	public abstract List<?> fetchUpdates(NotificationEntity entity);
	
	public abstract Intent notificationIntent(NotificationEntity entity);

	public int notificationId(NotificationEntity entity) {
		return (entity.id + entity.notificationClass).hashCode();
	}

	
	// can be overridden by subclasses
	public String notificationMessage(NotificationEntity entity) {
		return Utils.formatStringResource(context.getString(R.string.notification_message), 
				entity.results, entity.notificationName(), entity.name );
	}

	public String notificationTitle(NotificationEntity entity) {
		return Utils.capitalize(Utils.formatStringResource(context
				.getString(R.string.notification_title), entity
				.notificationName(), entity.name));
	}
}
