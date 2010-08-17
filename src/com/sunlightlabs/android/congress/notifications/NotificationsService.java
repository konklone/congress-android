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
import com.sunlightlabs.android.congress.BillLoader;
import com.sunlightlabs.android.congress.BillTabs;
import com.sunlightlabs.android.congress.Database;
import com.sunlightlabs.android.congress.LegislatorLoader;
import com.sunlightlabs.android.congress.LegislatorTabs;
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
		Log.d(Utils.TAG, "Creating notifications service and opening db");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		database.close();
		Log.d(Utils.TAG, "Destroying notifications service and closing db");
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		Log.d(Utils.TAG, "doWakefulWork()");

		registerUpdates(NotificationEntity.LEGISLATOR, new String[] { NotificationEntity.TWEETS,
				NotificationEntity.VIDEOS, NotificationEntity.NEWS, NotificationEntity.VOTES });

		registerUpdates(NotificationEntity.BILL, new String[] { NotificationEntity.NEWS,
				NotificationEntity.VOTES });
	}

	private void registerUpdates(String entityType, String[] notificationTypes) {
		for (String ntype : notificationTypes) {
			Cursor c = database.getNotifications(entityType, ntype);
			if (c.moveToFirst()) {
				do {
					NotificationEntity entity = database.loadEntity(c);
					
					if (ntype.equals(NotificationEntity.TWEETS))
						entity = processResults(new TwitterResultProcessor(this), entity);

					else if (ntype.equals(NotificationEntity.VIDEOS))
						entity = processResults(new YoutubeResultProcessor(this), entity);

					else if (ntype.equals(NotificationEntity.NEWS))
						entity = processResults(new YahooNewsResultProcessor(this), entity);

					else if (ntype.equals(NotificationEntity.VOTES)) {
						if(entityType.equals(NotificationEntity.LEGISLATOR))
							entity = processResults(new LegislatorVotesResultProcessor(this), entity);

						else if (entityType.equals(NotificationEntity.BILL))
							entity = processResults(new BillVotesResultProcessor(this), entity);
					}
					
					if(database.updateLastSeenNotification(entity.id, entity.notification_type, entity.lastSeenId) > 0) { 
						Log.d(Utils.TAG, "NotificationService: updated last seen id for entity " + entity.id);
						sendNotification(entity);
					}
					else 
						Log.d(Utils.TAG, "NotificationService: could not update the last seen id for entity " + entity.id);
						
				} while (c.moveToNext());
			}
			c.close();
		}
	}

	/**
	 * This method assumes that the results are ordered and the most recent one
	 * is the last in the list. If it's not the case, then it must be first
	 * sorted to match this criterion.
	 */
	protected NotificationEntity processResults(NotificationChecker checker, NotificationEntity entity) {
		final String id = entity.id;
		final String ntype = entity.notification_type;

		List<?> results = checker.callUpdate(entity.notification_data);

		if (results == null || results.isEmpty()) {
			Log.d(Utils.TAG, getClass().getSimpleName() + ": No " + ntype
					+ " to process for entity " + id);
		}

		final int size = results.size();
		Log.d(Utils.TAG, getClass().getSimpleName() + ": Loaded " + size + " " + ntype
				+ " for entity with id " + id);

		String lastId = checker.decodeId(results.get(size - 1));
		// search for the last seen id in the list of results, and calculate how
		// many new results are after that id
		if (entity.lastSeenId != null) {
			int foundPosition = -1;
			for (Object result : results) {
				if (entity.lastSeenId.equals(checker.decodeId(result))) {
					foundPosition = results.indexOf(result);
					break;
				}
			}

			if (foundPosition > -1) {
				entity.results = size - foundPosition - 1;
				Log.d(Utils.TAG, getClass().getSimpleName() + ": There are " + entity.results
						+ " *NEW* " + ntype + " for entity " + id);
			}
		} else
			// entity.lastSeenId is null, meaning it's the first time we check
			Log.d(Utils.TAG, getClass().getSimpleName() + ": First time check for entity " + id
					+ ", " + "set the last seen " + ntype + " id to " + lastId);

		// set the last seen id to the id of the most recent result
		entity.lastSeenId = lastId;
		return entity;
	}

	private void sendNotification(NotificationEntity entity) {
		if (entity.results > 0)
			notifyManager.notify(NOTIFY_UPDATES, getNotification(entity));
	}

	private Notification getNotification(NotificationEntity entity) {
		int icon = R.drawable.icon;
		CharSequence tickerText = getString(R.string.notification_ticker_text);
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		CharSequence contentTitle = String.format(getString(R.string.notification_title),
				entity.name);
		// TODO handle plural or singular results
		CharSequence contentText = entity.results + " new " + entity.notification_type;

		Intent notificationIntent = null;

		if (entity.type.equals("legislator")) {
			notificationIntent = new Intent(this, LegislatorLoader.class).putExtra(
					"legislator_id", entity.id).putExtra("tab",
					LegislatorTabs.Tabs.valueOf(entity.notification_type));
		} else if (entity.type.equals("bill")) {
			notificationIntent = new Intent(this, BillLoader.class).putExtra("id", entity.id)
					.putExtra("tab", BillTabs.Tabs.valueOf(entity.notification_type));
		}

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		return notification;
	}
}
