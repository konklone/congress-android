package com.sunlightlabs.android.congress.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class OnAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		WakefulIntentService.sendWakefulWork(context, NotificationsService.class);
	}

}
