package com.sunlightlabs.android.congress.notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.sunlightlabs.android.congress.Preferences;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;

public class Notifications {
	private static final String TAG = "CONGRESS";

	public static final String START_SERVICE_INTENT = "com.sunlightlabs.android.congress.intent.action.START_SERVICE";
	public static final String STOP_SERVICE_INTENT = "com.sunlightlabs.android.congress.intent.action.STOP_SERVICE";
	public static final int NOTIFY_UPDATES = 0;

	private static PendingIntent getPendingIntent(Context context) {
		Intent i = new Intent(context, OnAlarmReceiver.class);
		return PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
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
		Intent i = new Intent();
		i.setAction(Notifications.START_SERVICE_INTENT);
		context.sendBroadcast(i);
	}

	public static void stopNotificationsBroadcast(Context context) {
		Intent i = new Intent();
		i.setAction(Notifications.STOP_SERVICE_INTENT);
		context.sendBroadcast(i);
	}

	public static Notification getNotification(Context context) {
		Notification notification = new Notification(R.drawable.icon, "Title", System
				.currentTimeMillis());

		/*
		 * Intent notificationIntent = new Intent(context,
		 * LegislatorTabs.class).putExtra("tab", 1); PendingIntent contentIntent
		 * = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		 * notification.contentIntent = contentIntent; RemoteViews contentView =
		 * new RemoteViews("com.sunlightlabs.android.congress",
		 * R.layout.custom_notification);
		 * contentView.setTextViewText(R.id.title, "text");
		 * notification.contentView = contentView;
		 */
		return notification;
	}
}
