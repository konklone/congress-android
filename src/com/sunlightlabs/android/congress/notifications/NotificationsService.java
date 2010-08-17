package com.sunlightlabs.android.congress.notifications;

import java.util.ArrayList;
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

// apparently, we don't need async tasks to perform network calls because
// IntentService (and WakefulIntentService) already does its work on a 
// background thread; it is a bit like a regular service with an async task
// built-in

public class NotificationsService extends WakefulIntentService {
	public static final int NOTIFY_UPDATES = 0;

	private NotificationManager notifyManager;
	private Database database;
	private List<ResultProcessor> processors;

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

		processors = new ArrayList<ResultProcessor>();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		database.close();
		processors.clear();
		processors = null;
		Log.d(Utils.TAG, "Destroying notifications service and closing db");
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		Log.d(Utils.TAG, "doWakefulWork()");

		// to add another type of notification, register the entity type with
		// the corresponding notification types and create a new processor 
		// for that specific type of results; the code logic is encapsulated 
		// in the processor subclass, so no major changes in the service are required
		registerUpdates(NotificationEntity.LEGISLATOR, new String[] { NotificationEntity.TWEETS,
				NotificationEntity.VIDEOS, NotificationEntity.NEWS, NotificationEntity.VOTES });

		//registerUpdates(NotificationEntity.BILL, new String[] { NotificationEntity.NEWS,
				//NotificationEntity.VOTES });
	}

	private void registerUpdates(String entityType, String[] notificationTypes) {
		for (String ntype : notificationTypes) {
			Cursor c = database.getNotifications(entityType, ntype);
			if (c.moveToFirst()) {
				do {
					NotificationEntity entity = database.loadEntity(c);
					if (ntype.equals(NotificationEntity.TWEETS))
						processors.add(new TwitterResultProcessor(this, entity));

					else if (ntype.equals(NotificationEntity.VIDEOS))
						processors.add(new YoutubeResultProcessor(this, entity));

					else if (ntype.equals(NotificationEntity.NEWS))
						processors.add(new YahooNewsResultProcessor(this, entity));

					else if (ntype.equals(NotificationEntity.VOTES)) {
						if(entityType.equals(NotificationEntity.LEGISLATOR))
							processors.add(new LegislatorVotesResultProcessor(this, entity));

						else if (entityType.equals(NotificationEntity.BILL))
							processors.add(new BillBotesResultProcessor(this, entity));
					}
						
				} while (c.moveToNext());
			}
			c.close();
		}

		for (ResultProcessor processor : processors) {
			processor.callUpdate();
			NotificationEntity entity = processor.getEntity();

			if(database.updateLastSeenNotification(entity.id, entity.notification_type, entity.lastSeenId) > 0) { 
				Log.d(Utils.TAG, "NotificationService: updated last seen id for entity " + entity.id);
				sendNotification(entity);
			}
			else 
				Log.d(Utils.TAG, "NotificationService: could not update the last seen id for entity " + entity.id);
		}
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
