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
		// Debug.startMethodTracing("congress");
		notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		database = new Database(this);
		database.open();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		database.close();
		// Debug.stopMethodTracing();
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		Cursor c = database.getSubscriptions();
		
		if (c.moveToFirst()) {
			do {
				Subscription subscription = database.loadSubscription(c);
				
				try {
					NotificationFinder finder = (NotificationFinder) Class.forName(subscription.notificationClass).newInstance();
					finder.context = this;
					
					processResults(finder, subscription);
					
					if (subscription.lastSeenId != null) {
						
						if (database.updateLastSeenId(subscription) > 0) {
							if (subscription.results > 0) {
								doNotify(finder.notificationId(subscription), finder.notificationTitle(subscription), 
										 finder.notificationMessage(subscription), finder.notificationIntent(subscription),
										 subscription.results);
							}
							Log.i(Utils.TAG, "There are " + subscription.results + " new " + finder.getClass().getSimpleName() 
									+ " results for subscription " + subscription.id);
						}
						else
							Log.w(Utils.TAG, "Could not update last seen id for subscription " + subscription.id);
					}
					else
						Log.w(Utils.TAG, "Last seen id for subscription " + subscription.id + " is null!");
				} catch (Exception e) {
					Log.e(Utils.TAG, "Could not instatiate a NotificationFinder of class " + subscription.notificationClass, e);
				} 
			} while(c.moveToNext());
		}
		c.close();
	}

	/**
	 * This method assumes that the results are ordered and the most recent one
	 * is the last in the list. If it's not the case, then it must be first
	 * sorted to match this criterion.
	 */
	private void processResults(NotificationFinder finder, Subscription subscription) {
		String logCls = finder.getClass().getSimpleName();
		Log.d(Utils.TAG,  logCls + ": processing notifications for subscription " + subscription.id);
		
		List<?> results = finder.fetchUpdates(subscription);
		if (results == null || results.isEmpty()) return;
		
		int size = results.size();
		Log.d(Utils.TAG, logCls + ": there are " + size + " from the newtork call");

		// search for the last seen id in the list of results
		// and calculate how many new results are after that
		if (subscription.lastSeenId != null) {
			int foundIndex = -1;

			for (Object result : results) {
				if (subscription.lastSeenId.equals(finder.decodeId(result))) {
					foundIndex = results.indexOf(result);
					break;
				}
			}

			subscription.results = size - foundIndex - 1;
		}
		subscription.lastSeenId = finder.decodeId(results.get(size - 1));
		Log.i(Utils.TAG, logCls + ": last seen id for subscription " + subscription.id + " is updated to " + subscription.lastSeenId);
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
		
		PendingIntent contentIntent = PendingIntent
				.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);

		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.number = results;
		return notification;
	}
}
