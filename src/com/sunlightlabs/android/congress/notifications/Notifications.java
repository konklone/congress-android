package com.sunlightlabs.android.congress.notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.sunlightlabs.android.congress.LegislatorTabs;
import com.sunlightlabs.android.congress.Preferences;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;

public class Notifications {
	private static final String TAG = "CONGRESS";

	public static final String START_SERVICE = "com.sunlightlabs.android.congress.intent.action.START_SERVICE";
	public static final String STOP_SERVICE = "com.sunlightlabs.android.congress.intent.action.STOP_SERVICE";

	public static final int NOTIFY_UPDATES = 0;

	private static PendingIntent getPendingIntent(Context context) {
		Intent intent = new Intent(context, OnAlarmReceiver.class);
		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public static int getIntervalMillis(Context context) {
		return Integer.parseInt(Utils.getStringPreference(context,
						Preferences.KEY_NOTIFY_INTERVAL,
						Preferences.DEFAULT_NOTIFY_INTERVAL)) * 1000;
	}

	public static void scheduleNotifications(Context context) {
		Log.d(TAG, "Schedule notifications!");

		int interval = getIntervalMillis(context);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.currentThreadTimeMillis()
				+ interval, interval, getPendingIntent(context));
	}

	public static void stopNotifications(Context context) {
		Log.d(TAG, "Stop notifications!");

		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(getPendingIntent(context));
	}

	public static void startNotificationsBroadcast(Context context) {
		context.sendBroadcast(new Intent(Notifications.START_SERVICE));
	}

	public static void stopNotificationsBroadcast(Context context) {
		context.sendBroadcast(new Intent(Notifications.STOP_SERVICE));
	}

	public static Notification getNotification(Context context, NotificationEntity entity) {
		// TODO create a custom notification for each type of update
		int icon = R.drawable.icon;
		CharSequence tickerText = "New Updates for " + entity.name;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		CharSequence contentTitle = "New Updates for " + entity.name;
		CharSequence contentText = "There are " + entity.results + " new updates.";
		Intent notificationIntent = new Intent(context, LegislatorTabs.class).putExtra("tab", 2);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		return notification;
	}
}
