package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.sunlightlabs.android.congress.Database;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.finders.NotificationFinder;
import com.sunlightlabs.android.congress.utils.Utils;

public class NotificationsService extends WakefulIntentService {
	public static final int NOTIFY_UPDATES = 0;

	private NotificationManager notifyManager;
	private Database database;

	public NotificationsService() {
		super("NotificationService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		database = new Database(this);
		database.open();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		database.close();
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		Cursor c = database.getNotifications();
		Log.i(Utils.TAG, "There are currently " + c.getCount() + " notifications in the db!");

		if(c.moveToFirst()) {
			do {
				NotificationEntity entity = database.loadEntity(c);
				Log.d(Utils.TAG, "Processing notifications for entity " + entity);

				try {
					NotificationFinder finder = (NotificationFinder) Class.forName(entity.notificationClass).newInstance();
					finder.setContext(this);
					processResults(finder, entity);
					
					if (entity.lastSeenId != null) {
						//TODO change this to "> 0" (this is DEBUG)
						if(database.updateLastSeenId(entity) > 0 && entity.results == 0) 
							doNotify(finder.notificationId(entity),
									 finder.notificationTitle(entity), 
									 finder.notificationMessage(entity), 
									 finder.notificationIntent(entity),
									 entity.results);
					}
					else
						Log.w(Utils.TAG, "Could not update last seen id for entity " + entity.id);
				} catch (Exception e) {
					Log.e(Utils.TAG, "Could not instatiate a NotificationFinder of class " + entity.notificationClass);
				} 
			}while(c.moveToNext());
		}
	}

	/**
	 * This method assumes that the results are ordered and the most recent one
	 * is the last in the list. If it's not the case, then it must be first
	 * sorted to match this criterion.
	 */
	private void processResults(NotificationFinder finder, NotificationEntity entity) {
		List<?> results = finder.callUpdate(entity);

		if (results == null || results.isEmpty()) return;
		int size = results.size();

		// search for the last seen id in the list of results
		// and calculate how many new results are after that
		if (entity.lastSeenId != null) {
			int foundIndex = -1;

			for (Object result : results) {
				if (entity.lastSeenId.equals(finder.decodeId(result))) {
					foundIndex = results.indexOf(result);
					break;
				}
			}

			entity.results = size - foundIndex - 1;
		}

		entity.lastSeenId = finder.decodeId(results.get(size - 1));
	}

	private void doNotify(int id, String title, String message, Intent intent, int results) {
		notifyManager.notify(id, getNotification(title, message, intent, results));
	}

	private Notification getNotification(String title, String message, Intent intent, int results) {
		int icon = R.drawable.icon;
		CharSequence tickerText = getString(R.string.notification_ticker_text);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);

		CharSequence contentTitle = title;
		CharSequence contentText = message;
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);

		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.number = results;
		return notification;
	}
}
