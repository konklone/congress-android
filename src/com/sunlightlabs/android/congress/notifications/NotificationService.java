package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.sunlightlabs.android.congress.Database;
import com.sunlightlabs.android.congress.NotificationSettings;
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
			Subscriber subscriber;
			try {
				subscriber = (Subscriber) Class.forName("com.sunlightlabs.android.congress.notifications.subscribers." + subscription.notificationClass).newInstance();
				subscriber.context = this;
			} catch (Exception e) {
				Log.e(Utils.TAG, "Could not instantiate a Subscriber of class " + subscription.notificationClass, e);
				continue;
			}
			
			Log.i(Utils.TAG, "[" + subscriber.getClass().getSimpleName() + "][" + subscription.id + "] - " +
				"About to fetch updates.");
			
			
			// ask the finder for the latest updates
			List<?> updates = subscriber.fetchUpdates(subscription);
			
			
			// if there was an error or there were no results, move on
			if (updates == null || updates.isEmpty()) {
				Log.i(Utils.TAG, "[" + subscriber.getClass().getSimpleName() + "][" + subscription.id + "] - " +
					"No results, or an error - moving on and not notifying.");
				continue;
			}
			
			// cache the lastSeenId of the subscription from its previous run
			String oldLastSeenId = subscription.lastSeenId;
			
			// No matter what, update the database to set lastSeenId as the ID of the first update
			String newLastSeenId = subscriber.decodeId(updates.get(0));
			database.updateLastSeenId(subscription, newLastSeenId);
			
			
			// Scan through the updates for a match of the last seen ID 
			int results = -1;
			if (oldLastSeenId != null) {
				for (Object update : updates) {
					String id = subscriber.decodeId(update);
					if (oldLastSeenId.equals(id)) {
						results = updates.indexOf(update);
						break;
					}
				}
			}
			
			// if not matched, all of them must be new
			if (results == -1) {
				results = updates.size();
				
				if (oldLastSeenId == null)
					Log.i(Utils.TAG, "[" + subscriber.getClass().getSimpleName() + "][" + subscription.id + "] - " +
						"No lastSeenId, will notify of all results.");
				else
					Log.i(Utils.TAG, "[" + subscriber.getClass().getSimpleName() + "][" + subscription.id + "] - " +
						"Have lastSeenId (" + oldLastSeenId + "), but it did not appear, will notify of all results");
			}
			
			// if there's at least one new item, notify the user
			if (results > 0) {
				
				notifyManager.notify(
					(subscription.id + subscription.notificationClass).hashCode(), 
					getNotification(
						subscriber.notificationTicker(subscription),
						subscriber.notificationTitle(subscription), 
						subscriber.notificationMessage(subscription, results), 
						subscriber.notificationIntent(subscription),
						
						notificationUri(subscription),
						results
					)
				);
				
				Log.i(Utils.TAG, "[" + subscriber.getClass().getSimpleName() + "][" + subscription.id + "] - " +
					"notified of " + results + " results, " +
					"oldLastSeenId was " + (oldLastSeenId != null ? oldLastSeenId : "null") + ", newlastSeenId is " + newLastSeenId);
			} else
				Log.i(Utils.TAG, "[" + subscriber.getClass().getSimpleName() + "][" + subscription.id + "] - " +
						"0 new results, not notifying.");
			
		} while(cursor.moveToNext());
		
		cursor.close();
	}

	private Notification getNotification(String ticker, String title, String message, Intent intent, Uri uri, int results) {
		int icon = R.drawable.icon;
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, ticker, when);

		intent.setData(uri);
		
		PendingIntent contentIntent = PendingIntent
				.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(this, title, message, contentIntent);
		
		// Attach notification sound if the user picked one (defaults to silent)
		String ringtone = PreferenceManager.getDefaultSharedPreferences(this).getString(NotificationSettings.KEY_NOTIFY_RINGTONE, NotificationSettings.DEFAULT_NOTIFY_RINGTONE);
		if (ringtone != null)
			notification.sound = Uri.parse(ringtone);
		
		// Vibrate unless user disabled it
		boolean vibration = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(NotificationSettings.KEY_NOTIFY_VIBRATION, NotificationSettings.DEFAULT_NOTIFY_VIBRATION);
		if (vibration)
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		
		// always show the light
		notification.ledARGB = 0xffffffff;
		notification.ledOnMS = 2000;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;

		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.number = results;
		
		return notification;
	}
	
	// hack to make sure PendingIntents are always recognized as unique
	private Uri notificationUri(Subscription subscription) {
		return Uri.parse("congress://notifications/" + subscription.notificationClass + "/" + subscription.id);
	}
}