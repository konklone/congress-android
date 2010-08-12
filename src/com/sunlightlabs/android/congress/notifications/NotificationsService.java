package com.sunlightlabs.android.congress.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import winterwell.jtwitter.Twitter.Status;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.sunlightlabs.android.congress.Database;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.tasks.LoadTweetsTask;
import com.sunlightlabs.android.congress.tasks.LoadYahooNewsTask;
import com.sunlightlabs.android.congress.tasks.LoadYoutubeVideosTask;
import com.sunlightlabs.android.congress.tasks.LoadTweetsTask.LoadsTweets;
import com.sunlightlabs.android.congress.tasks.LoadYahooNewsTask.LoadsYahooNews;
import com.sunlightlabs.android.congress.tasks.LoadYoutubeVideosTask.LoadsYoutubeVideos;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.youtube.Video;

public class NotificationsService extends WakefulIntentService implements LoadsTweets,
		LoadsYoutubeVideos, LoadsYahooNews {
	private static final String TAG = "CONGRESS";

	private NotificationManager notifyManager;
	private Database database;

	private Map<String, NotificationEntity> entities;

	private enum UpdateType {
		twitter, youtube, news;
	}

	public NotificationsService() {
		super("NotificationService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		database = new Database(this);
		database.open();
		entities = new HashMap<String, NotificationEntity>();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		database.close();
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		// check legislator updates
		checkUpdates("legislator");

		// check bill updates
		checkUpdates("bill");

		// check laws updates
	}


	private void notify(NotificationEntity e) {
		if (e.results > 0)
			notifyManager.notify(Notifications.NOTIFY_UPDATES, Notifications.getNotification(this, e));
		entities.remove(e.id);
	}

	private void checkUpdates(String entityType) {
		for (int i = 0; i < UpdateType.values().length; i++) {
			UpdateType updateType = UpdateType.values()[i];

			Cursor c = database.getNotifications(entityType, updateType.name());
			if (c.moveToFirst()) {
				do {
					NotificationEntity e = database.loadEntity(c);
					Log.d(TAG, "Checking " + updateType.name() + " updates for entity " + e.id);

					switch (updateType) {
					case twitter:
						new LoadTweetsTask(this, e.id).execute(e.notificationData);
						break;
					case youtube:
						new LoadYoutubeVideosTask(this, e.id).execute(e.notificationData);
						break;
					case news:
						new LoadYahooNewsTask(this, e.id).execute(e.notificationData,
								getResources().getString(R.string.yahoo_news_key));
						break;
					}

					// keep this in the map, so when results are loaded, we'll
					// know for which entity to send the notification
					if (!entities.containsKey(e.id))
						entities.put(e.id, e);
				} while (c.moveToNext());
			}
			c.close();
		}
	}


	public void onLoadTweets(List<Status> tweets, String... id) {
		String eId = id[0];
		NotificationEntity e = entities.get(eId);

		if (tweets != null && !tweets.isEmpty()) {
			Log.d(TAG, "Loaded " + tweets.size() + " tweets for entity with id " + eId);

			// first time don't send a notification, the user sees the tweets
			// on the profile
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
			e.results = tweets.size() - pos - 1;

			Log.d(TAG, "Last seen tweet for entity " + e.id + " is " + e.lastSeenId
					+ ". There are " + e.results + " new ones");
			long ok = database.updateLastSeenNotification(e.lastSeenId, e.notificationType,
					e.lastSeenId);
			if (ok == 0) {
				Log.w(TAG, "Could not update last seen twitter id for entity " + e.toString());
			}

			notify(e);
		}
	}

	public void onLoadYoutubeVideos(Video[] videos, String... id) {
		String eId = id[0];
		NotificationEntity e = entities.get(eId);

		if (videos != null && videos.length > 0) {
			Log.d(TAG, "Loaded " + videos.length + " youtube videos for entity with id " + eId);

			if (e.lastSeenId == null) {
				e.lastSeenId = videos[videos.length - 1].timestamp.toString();
				return;
			}

			int i = 0;
			int pos = -1;
			for (i = 0; i < videos.length; i++) {
				if (e.lastSeenId.equals(videos[i].timestamp.toString())) {
					pos = i;
					break;
				}
			}

			e.lastSeenId = videos[videos.length - 1].timestamp.toString();
			e.results = videos.length - pos - 1;

			Log.d(TAG, "Last seen video for entity " + e.id + " is " + e.lastSeenId
					+ ". There are " + e.results + " new ones");
			long ok = database.updateLastSeenNotification(e.lastSeenId, e.notificationType,
					e.lastSeenId);
			if (ok == 0) {
				Log.w(TAG, "Could not update last seen youtube video for entity " + e.toString());
			}

			notify(e);
		}
	}

	public void onLoadYahooNews(ArrayList<NewsItem> news, String... id) {
		String eId = id[0];
		NotificationEntity e = entities.get(eId);

		if (news != null && news.size() > 0) {
			Log.d(TAG, "Loaded yahoo news for entity with id " + eId);

			if (e.lastSeenId == null) {
				e.lastSeenId = news.get(news.size() - 1).timestamp.toString();
				return;
			}

			int i = 0;
			int pos = -1;
			for (i = 0; i < news.size(); i++) {
				if (e.lastSeenId.equals(news.get(i).timestamp.toString())) {
					pos = i;
					break;
				}
			}

			e.lastSeenId = news.get(news.size() - 1).timestamp.toString();
			e.results = news.size() - pos - 1;

			Log.d(TAG, "Last seen news for entity " + e.id + " is " + e.lastSeenId + ". There are "
					+ e.results + " new ones");
			long ok = database.updateLastSeenNotification(e.lastSeenId, e.notificationType,
					e.lastSeenId);
			if (ok == 0) {
				Log.w(TAG, "Could not update last seen youtube video for entity " + e.toString());
			}

			notify(e);
		}
	}
}
