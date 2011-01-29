package com.sunlightlabs.android.congress.notifications;

import java.util.ArrayList;
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
				subscriber = subscription.getSubscriber();
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
			
			
			// keep a record of the un-seen matches as we go through the updates
			List<String> unseenIds = new ArrayList<String>();
			
			int size = updates.size();
			for (int i=0; i<size; i++) {
				String itemId = subscriber.decodeId(updates.get(i));
				if (!database.hasSubscriptionItem(subscription.id, subscription.notificationClass, itemId))
					unseenIds.add(itemId);
			}
			
			database.addSubscription(subscription, unseenIds);
			
			int results = unseenIds.size();
			
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
					"notified of " + results + " results");
			} else
				Log.i(Utils.TAG, "[" + subscriber.getClass().getSimpleName() + "][" + subscription.id + "] - " +
						"0 new results, not notifying.");
			
		} while(cursor.moveToNext());
		
		cursor.close();
	}

	private Notification getNotification(String ticker, String title, String message, Intent intent, Uri uri, int results) {
		int icon = R.drawable.notification_icon;
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
		
		if (results > 1)
			notification.number = results;
		
		return notification;
	}
	
	// hack to make sure PendingIntents are always recognized as unique
	private Uri notificationUri(Subscription subscription) {
		return Uri.parse("congress://notifications/" + subscription.notificationClass + "/" + subscription.id);
	}
}