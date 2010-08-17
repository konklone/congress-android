package com.sunlightlabs.android.congress.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.Preferences;
import com.sunlightlabs.android.congress.utils.Utils;

public class OnBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

			if (Utils.getBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED, Preferences.DEFAULT_NOTIFY_ENABLED)) {
				Utils.startNotificationsBroadcast(context);
				Log.d(Utils.TAG, "OnAlarmReceiver: boot completed, started notification service (prefs are ON).");
			}
			else
				Log.d(Utils.TAG, "OnAlarmReceiver: boot completed, notification service not started (prefs are OFF)");
		}
	}
}
