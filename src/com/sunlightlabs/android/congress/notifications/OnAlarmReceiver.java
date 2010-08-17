package com.sunlightlabs.android.congress.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.sunlightlabs.android.congress.utils.Utils;

public class OnAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		WakefulIntentService.sendWakefulWork(context, NotificationsService.class);
		Log.d(Utils.TAG, "OnAlarmReceiver: waking the notification service to do its job!");
	}
}
