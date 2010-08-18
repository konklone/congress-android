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
import com.sunlightlabs.android.congress.notifications.finders.BillActionsFinder;
import com.sunlightlabs.android.congress.notifications.finders.BillVotesFinder;
import com.sunlightlabs.android.congress.notifications.finders.LegislatorVotesFinder;
import com.sunlightlabs.android.congress.notifications.finders.TwitterFinder;
import com.sunlightlabs.android.congress.notifications.finders.YahooNewsFinder;
import com.sunlightlabs.android.congress.notifications.finders.YoutubeFinder;
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
		Log.d(Utils.TAG, "================ NotificationService Start ================");

		registerUpdates(NotificationEntity.LEGISLATOR, new String[] { NotificationEntity.TWEETS,
				NotificationEntity.VIDEOS, NotificationEntity.NEWS, NotificationEntity.VOTES });

		registerUpdates(NotificationEntity.BILL, new String[] { NotificationEntity.NEWS,
				NotificationEntity.VOTES, NotificationEntity.ACTIONS });

		Log.d(Utils.TAG, "================ NotificationService End  ================ \n\n");
	}

	private void registerUpdates(String entityType, String[] notificationTypes) {
		for (String ntype : notificationTypes) {

			Cursor c = database.getNotifications(entityType, ntype);
			if (c.moveToFirst()) {
				do {
					NotificationEntity entity = database.loadEntity(c);

					if (ntype.equals(NotificationEntity.TWEETS))
						entity = processResults(new TwitterFinder(this), entity);

					else if (ntype.equals(NotificationEntity.VIDEOS))
						entity = processResults(new YoutubeFinder(this), entity);

					else if (ntype.equals(NotificationEntity.NEWS))
						entity = processResults(new YahooNewsFinder(this), entity);

					else if (ntype.equals(NotificationEntity.VOTES)) {
						if (entityType.equals(NotificationEntity.LEGISLATOR))
							entity = processResults(new LegislatorVotesFinder(this), entity);

						else if (entityType.equals(NotificationEntity.BILL))
							entity = processResults(new BillVotesFinder(this), entity);
					}

					else if (ntype.equals(NotificationEntity.ACTIONS))
						entity = processResults(new BillActionsFinder(this), entity);

					// update the last seen id and send a notification if there are new updates
					long ok = 0;
					if (entity.lastSeenId != null) {
						ok = database.updateLastSeenNotification(entity);
						if (ok > 0 && entity.results > 0)
							doNotify(entity);
					}
					if (ok == 0)
						Log.w(Utils.TAG, "Could not update last seen id for entity " + entity.id);

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
	protected NotificationEntity processResults(NotificationFinder finder, NotificationEntity entity) {
		final String id = entity.id;
		final String ntype = entity.notificationType;

		List<?> results = finder.callUpdate(entity.notificationData);

		// return entity unchanged
		if (results == null || results.isEmpty()) {
			Log.d(Utils.TAG, "No " + ntype + " to process for entity " + id);
			return entity;
		}

		final int size = results.size();
		Log.d(Utils.TAG, "Loaded " + size + " " + ntype + " for entity with id " + id);

		// search for the last seen id in the list of results, and calculate how
		// many new results are after that
		if (entity.lastSeenId != null) {
			int foundPosition = -1;
			for (Object result : results) {
				if (entity.lastSeenId.equals(finder.decodeId(result))) {
					foundPosition = results.indexOf(result);
					break;
				}
			}

			if (foundPosition > -1) {
				entity.results = size - foundPosition - 1;
				Log.d(Utils.TAG, "There are " + entity.results + " *new* " + ntype + " for entity "
						+ id);
			}
		}

		// set the last seen id to the id of the most recent result
		entity.lastSeenId = finder.decodeId(results.get(size - 1));
		Log.d(Utils.TAG, "Set the last seen " + ntype + " id to " + entity.lastSeenId + " for entity " + id);

		return entity;
	}

	private void doNotify(NotificationEntity entity) {
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
		CharSequence contentText = entity.results + " new " + entity.notificationType;

		Intent notificationIntent = null;

		if (entity.type.equals("legislator")) {
			notificationIntent = new Intent(this, LegislatorLoader.class).putExtra("legislator_id",
					entity.id)
					.putExtra("tab", LegislatorTabs.Tabs.valueOf(entity.notificationType));
		} else if (entity.type.equals("bill")) {
			notificationIntent = new Intent(this, BillLoader.class).putExtra("id", entity.id)
					.putExtra("tab", BillTabs.Tabs.valueOf(entity.notificationType));
		}

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		return notification;
	}
}
