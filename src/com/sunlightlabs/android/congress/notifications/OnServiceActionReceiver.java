package com.sunlightlabs.android.congress.notifications;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.NotificationSettings;
import com.sunlightlabs.android.congress.utils.Utils;

/**
 * The service must be stopped from the same context from which it was started.
 * That's why we send a broadcast every time we need to perform a start/stop
 * action on the service, to use the same context (in which the receiver is
 * running)
 * 
 */
public class OnServiceActionReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (action.equals(Utils.START_NOTIFICATION_SERVICE)) {
			scheduleNotifications(context);
		} else if (action.equals(Utils.STOP_NOTIFICATION_SERVICE)) {
			stopNotifications(context);
		}
	}

	private static PendingIntent getPendingIntent(Context context) {
		Intent intent = new Intent(context, OnAlarmReceiver.class);
		intent.setAction(Utils.START_NOTIFICATION_SERVICE);
		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private static void scheduleNotifications(Context context) {
		int interval = Integer.parseInt(Utils.getStringPreference(context,
				NotificationSettings.KEY_NOTIFY_INTERVAL, NotificationSettings.DEFAULT_NOTIFY_INTERVAL));

		Log.d(Utils.TAG, "OnServiceActionReceiver: Schedule notifications to repeat in " + interval + " minutes.");
		Calendar c = Calendar.getInstance();
		c.setTime(new Date()); // set time to now
		c.add(Calendar.MINUTE, interval);
		interval *= 60000; // convert to milliseconds
		
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		// if the interval is 15 minutes or tighter, use inexact alarms to conserve on battery
		if (interval <= (15 * 60000)) {
			am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), interval,	getPendingIntent(context));
		} else {
			am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), interval,	getPendingIntent(context));
		}
	}

	private static void stopNotifications(Context context) {
		Log.d(Utils.TAG, "OnServiceActionReceiver: Stop notifications.");

		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(getPendingIntent(context));
	}

}