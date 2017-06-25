package com.sunlightlabs.android.congress.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.NotificationSettings;
import com.sunlightlabs.android.congress.utils.Utils;

public class OnBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

			if (Utils.getBooleanPreference(context, NotificationSettings.KEY_NOTIFY_ENABLED, NotificationSettings.DEFAULT_NOTIFY_ENABLED)) {
				Utils.startNotificationsBroadcast(context);
				Log.d(Utils.TAG, "OnBootReceiver: boot completed, started notification service (prefs are ON).");
			}
			else
				Log.d(Utils.TAG, "OnBootReceiver: boot completed, notification service not started (prefs are OFF)");
		}
	}
}