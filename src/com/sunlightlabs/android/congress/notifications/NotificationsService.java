package com.sunlightlabs.android.congress.notifications;

import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class NotificationsService extends WakefulIntentService {

	public NotificationsService() {
		super("NotificationService");
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		Log.d("CONGRESS", "Do service work!!!");
	}
}
