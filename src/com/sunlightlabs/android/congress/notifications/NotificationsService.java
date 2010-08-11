package com.sunlightlabs.android.congress.notifications;

import java.util.HashMap;
import java.util.List;

import winterwell.jtwitter.Twitter.Status;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.sunlightlabs.android.congress.Database;
import com.sunlightlabs.android.congress.utils.LoadTweetsTask;
import com.sunlightlabs.android.congress.utils.LoadTweetsTask.LoadsTweets;

public class NotificationsService extends WakefulIntentService implements LoadsTweets {
	private static final String TAG = "CONGRESS";

	private NotificationManager notifyManager;
	private Database database;

	private HashMap<String, NotifiableEntity> entities;

	public NotificationsService() {
		super("NotificationService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		database = new Database(this);
		database.open();

		entities = new HashMap<String, NotifiableEntity>();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		database.close();
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		// check legislator updates
		checkTwitterUpdates();

		// check bill updates

		// check laws updates
	}

	private void checkTwitterUpdates() {
		Cursor c = database.getNotifications("legislator", "twitter");

		if (c.moveToFirst()) {
			do {
				NotifiableEntity e = database.loadEntity(c);

				LoadTweetsTask task = new LoadTweetsTask(this, e.id);
				task.execute(e.notificationData);

				if (!entities.containsKey(e.id))
					entities.put(e.id, e);

			} while (c.moveToNext());
		}

		c.close();
	}

	public void onLoadTweets(List<Status> tweets, String... id) {
		String eId = id[0];
		
		if (tweets.size() > 0) {
			NotifiableEntity e = entities.get(eId);

			if (e.lastSeenId == null) {
				e.lastSeenId = new Long(tweets.get(tweets.size() - 1).id).toString();
				return;
			}

			int i = 0;
			int pos = -1;
			for (i = 0; i < tweets.size(); i++) {
				if (e.lastSeenId.equals(new Long(tweets.get(i).id).toString())) {
					pos = i;
					break;
				}
			}

			e.lastSeenId = new Long(tweets.get(tweets.size() - 1).id).toString();
			// e.results = tweets.size() - pos - 1;
			e.results = 5;

			if (e.results > 0) {
				Log.d(TAG, "Found new " + e.results + "tweets!");
				notifyManager.notify(Notifications.NOTIFY_UPDATES, Notifications
						.getTwitterNotification(this, e));
			}
		}
	}
}
