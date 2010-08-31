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
		Cursor cursor = database.getSubscriptions();
		
		if (!cursor.moveToFirst()) {
			cursor.close();
			return;
		}
		
		do {
			Subscription subscription = database.loadSubscription(cursor);
			
			// load the appropriate finder for this subscription 
			NotificationFinder finder;
			try {
				finder = (NotificationFinder) Class.forName(subscription.notificationClass).newInstance();
				finder.context = this;
			} catch (Exception e) {
				Log.e(Utils.TAG, "Could not instantiate a NotificationFinder of class " + subscription.notificationClass, e);
				continue;
			}
			
			
			// ask the finder for the latest updates
			List<?> updates = finder.fetchUpdates(subscription);
			if (updates == null || updates.isEmpty())
				continue;
			
			String lastSeenId = null;
			
			// Scan through the updates for a match of the last seen ID 
			int results = -1;
			for (Object update : updates) {
				String id = finder.decodeId(update);
				if (subscription.lastSeenId.equals(id)) {
					results = updates.indexOf(update);
					lastSeenId = id;
					break;
				}
			}
			
			// if not matched, all of them must be new
			if (results == -1) {
				results = updates.size();
				lastSeenId = finder.decodeId(updates.get(0));
			}
			
			// if there's at least one new item, notify the user
			if (results > 0) {
				
				notifyManager.notify(
					finder.notificationId(subscription), 
					getNotification(
						finder.notificationTitle(subscription), 
						finder.notificationMessage(subscription, results), 
						finder.notificationIntent(subscription),
						results
					)
				);
				
				Log.i(Utils.TAG, "There are " + results + " new " + finder.getClass().getSimpleName() 
						+ " results for subscription " + subscription.id);
				
				database.updateLastSeenId(subscription, lastSeenId);
			}
			
		} while(cursor.moveToNext());
		
		cursor.close();
	}


	private Notification getNotification(String title, String message, Intent intent, int results) {
		int icon = R.drawable.icon;
		
		CharSequence tickerText = getString(R.string.notification_ticker_text);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);

		PendingIntent contentIntent = PendingIntent
				.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(this, title, message, contentIntent);

		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.number = results;
		return notification;
	}
}