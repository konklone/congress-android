package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.content.Context;

public abstract class NotificationChecker {
	protected Context context;

	public NotificationChecker(Context context) {
		this.context = context;
	}

	public abstract List<?> callUpdate(String data);
	
	protected abstract String decodeId(Object result);

	
}
