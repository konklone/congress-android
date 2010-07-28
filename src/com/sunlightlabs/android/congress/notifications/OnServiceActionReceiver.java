package com.sunlightlabs.android.congress.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnServiceActionReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (action.equals(Notifications.START_SERVICE_INTENT))
			Notifications.scheduleNotifications(context);
		else if (action.equals(Notifications.STOP_SERVICE_INTENT))
			Notifications.stopNotifications(context);
	}
}
