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

public class NotificationService extends WakefulIntentService {
	public static final int NOTIFY_UPDATES = 0;

	private NotificationManager notifyManager;
	private Database database;

	public NotificationService() {
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
				finder = (NotificationFinder) Class.forName("com.sunlightlabs.android.congress.notifications.finders." + subscription.notificationClass).newInstance();
				finder.context = this;
			} catch (Exception e) {
				Log.e(Utils.TAG, "Could not instantiate a NotificationFinder of class " + subscription.notificationClass, e);
				continue;
			}
			
			Log.i(Utils.TAG, "[" + finder.getClass().getSimpleName() + "][" + subscription.id + "] - " +
				"About to fetch updates.");
			
			
			// ask the finder for the latest updates
			List<?> updates = finder.fetchUpdates(subscription);
			
			
			// if there were no results, move on
			if (updates == null || updates.isEmpty())
				continue;
			
			// cache the lastSeenId of the subscription from its previous run
			String oldLastSeenId = subscription.lastSeenId;
			
			// No matter what, update the database to set lastSeenId as the ID of the first update
			String newLastSeenId = finder.decodeId(updates.get(0));
			database.updateLastSeenId(subscription, newLastSeenId);
			

			// if the subscription has no lastSeenId, then this is its first run,
			// no need to go on to notifying
			if (oldLastSeenId == null) {
				Log.i(Utils.TAG, "[" + finder.getClass().getSimpleName() + "][" + subscription.id + "] - " +
						"No lastSeenId, assuming first run and not notifying.");
				continue;
			}
			
			// Scan through the updates for a match of the last seen ID 
			int results = -1;
			for (Object update : updates) {
				String id = finder.decodeId(update);
				if (oldLastSeenId.equals(id)) {
					results = updates.indexOf(update);
					break;
				}
			}
			
			// if not matched, all of them must be new
			if (results == -1) {
				results = updates.size();
				
				Log.i(Utils.TAG, "[" + finder.getClass().getSimpleName() + "][" + subscription.id + "] - " +
					"Did not match lastSeenId, assuming all updates are new.");
			}
			
			// if there's at least one new item, notify the user
			if (results >= 0) {
				
				notifyManager.notify(
					(subscription.id + subscription.notificationClass).hashCode(), 
					getNotification(
						finder.notificationTicker(subscription),
						finder.notificationTitle(subscription), 
						finder.notificationMessage(subscription, results), 
						finder.notificationIntent(subscription),
						
						results
					)
				);
				
				Log.i(Utils.TAG, "[" + finder.getClass().getSimpleName() + "][" + subscription.id + "] - " +
					"notified of " + results + " results.");
			} else
				Log.i(Utils.TAG, "[" + finder.getClass().getSimpleName() + "][" + subscription.id + "] - " +
						"0 new results, not notifying.");
			
		} while(cursor.moveToNext());
		
		cursor.close();
	}

	private Notification getNotification(String ticker, String title, String message, Intent intent, int results) {
		int icon = R.drawable.icon;
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, ticker, when);

		PendingIntent contentIntent = PendingIntent
				.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(this, title, message, contentIntent);

		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.number = results;
		
		return notification;
	}
}