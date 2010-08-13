package com.sunlightlabs.android.congress.notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.sunlightlabs.android.congress.BillLoader;
import com.sunlightlabs.android.congress.BillTabs;
import com.sunlightlabs.android.congress.LegislatorLoader;
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
		int icon = R.drawable.icon;
		CharSequence tickerText = context.getString(R.string.notification_ticker_text);
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		CharSequence contentTitle = String.format(context.getString(R.string.notification_title), entity.name);
		// TODO handle plural or singular results
		CharSequence contentText = entity.results + " new " + entity.notificationType.name();
		
		Intent notificationIntent = null;
		
		if(entity.type.equals("legislator")) {
			notificationIntent = new Intent(context, LegislatorLoader.class)
				.putExtra("legislator_id", entity.id)
				.putExtra("tab", LegislatorTabs.Tabs.valueOf(entity.notificationType.name()));
		}
		else if(entity.type.equals("bill")) {
			notificationIntent = new Intent(context, BillLoader.class).putExtra("id", entity.id)
					.putExtra("tab", BillTabs.Tabs.valueOf(entity.notificationType.name()));
		}
				
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		return notification;
	}
}
