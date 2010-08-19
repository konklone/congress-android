package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import android.content.Context;
import android.content.Intent;

import com.sunlightlabs.android.congress.notifications.NotificationEntity;

public abstract class NotificationFinder {
	protected Context context;

	public NotificationFinder() {}

	public void setContext(Context context) {
		this.context = context;
	}

	public abstract List<?> callUpdate(NotificationEntity entity);
	
	public abstract String decodeId(Object result);

	public abstract Intent notificationIntent(NotificationEntity entity);

	public abstract String notificationMessage(NotificationEntity entity);

	public abstract String notificationTitle(NotificationEntity entity);
}
