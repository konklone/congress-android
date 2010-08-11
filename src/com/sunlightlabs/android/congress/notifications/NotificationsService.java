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
import com.sunlightlabs.android.congress.utils.LoadTweetsTask;
import com.sunlightlabs.android.congress.utils.LoadYahooNewsTask;
import com.sunlightlabs.android.congress.utils.LoadYoutubeVideosTask;
import com.sunlightlabs.android.congress.utils.LoadTweetsTask.LoadsTweets;
import com.sunlightlabs.android.congress.utils.LoadYahooNewsTask.LoadsYahooNews;
import com.sunlightlabs.android.congress.utils.LoadYoutubeVideosTask.LoadsYoutubeVideos;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.youtube.Video;

public class NotificationsService extends WakefulIntentService implements LoadsTweets,
		LoadsYoutubeVideos, LoadsYahooNews {
	private static final String TAG = "CONGRESS";

	private static final int LEGISLATOR_UPDATES_COUNT = 3;
	private static final int BILL_UPDATES_COUNT = 1;

	private NotificationManager notifyManager;
	private Database database;

	private Map<String, Map<String, NotificationResult>> updates;
	private Map<String, Integer> counts;

	public NotificationsService() {
		super("NotificationService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		database = new Database(this);
		database.open();

		updates = new HashMap<String, Map<String, NotificationResult>>();
		updates.put("twitter", new HashMap<String, NotificationResult>());
		updates.put("youtube", new HashMap<String, NotificationResult>());
		updates.put("news", new HashMap<String, NotificationResult>());

		counts = new HashMap<String, Integer>();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		database.close();
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		// check legislator updates
		checkLegislatorTwitterUpdates();
		checkLegislatorYoutubeUpdates();
		checkLegislatorNewsUpdates();

		// check bill updates
		checkBillNewsUpdates();

		// check laws updates
	}

	private synchronized void incrementUpdateCounts(NotificationResult e) {
		String id = e.id;
		if (counts.containsKey(id))
			counts.put(id, counts.get(id) + 1);
		else
			counts.put(id, 1);

		String type = e.type;
		int count = counts.get(id);
		if ((type.equals("legislator") && count == LEGISLATOR_UPDATES_COUNT)
				|| (type.equals("bill") && count == BILL_UPDATES_COUNT))
			sendLegislatorNotification(e);
	}

	private void sendLegislatorNotification(NotificationResult e) {
		updates.get(e.notificationType).remove(e.id);

		if (e.results > 0) {
			Log.d(TAG, "Found new " + e.results + "results!");
			notifyManager.notify(Notifications.NOTIFY_UPDATES, Notifications
					.getTwitterNotification(this, e));
		}
	}

	private void checkLegislatorTwitterUpdates() {
		Cursor c = database.getNotifications("legislator", "twitter");

		if (c.moveToFirst()) {
			do {
				NotificationResult e = database.loadEntity(c);
				Log.d(TAG, "Checking twitter updates for entity " + e.toString());

				LoadTweetsTask task = new LoadTweetsTask(this, e.id);
				task.execute(e.notificationData);

				Map<String, NotificationResult> twitterUpdates = updates.get("twitter");
				if (!twitterUpdates.containsKey(e.id))
					twitterUpdates.put(e.id, e);

			} while (c.moveToNext());
		}
		c.close();
	}

	private void checkLegislatorYoutubeUpdates() {
		Cursor c = database.getNotifications("legislator", "youtube");

		if (c.moveToFirst()) {
			do {
				NotificationResult e = database.loadEntity(c);
				Log.d(TAG, "Checking youtube updates for entity " + e.toString());

				LoadYoutubeVideosTask task = new LoadYoutubeVideosTask(this, e.id);
				task.execute(e.notificationData);

				Map<String, NotificationResult> youtubeUpdates = updates.get("youtube");
				if (!youtubeUpdates.containsKey(e.id))
					youtubeUpdates.put(e.id, e);

			} while (c.moveToNext());
		}
		c.close();
	}

	private void checkLegislatorNewsUpdates() {
		Cursor c = database.getNotifications("legislator", "news");

		if (c.moveToFirst()) {
			do {
				NotificationResult e = database.loadEntity(c);
				Log.d(TAG, "Checking yahoo news updates for entity " + e.toString());

				LoadYahooNewsTask task = new LoadYahooNewsTask(this, e.id);
				String apiKey = getResources().getString(R.string.yahoo_news_key);
				task.execute(e.notificationData, apiKey);

				Map<String, NotificationResult> newsUpdates = updates.get("news");
				if (!newsUpdates.containsKey(e.id))
					newsUpdates.put(e.id, e);

			} while (c.moveToNext());
		}
		c.close();
	}

	private void checkBillNewsUpdates() {
		Cursor c = database.getNotifications("bill", "news");

		if (c.moveToFirst()) {
			do {
				NotificationResult e = database.loadEntity(c);
				Log.d(TAG, "Checking yahoo news updates for entity " + e.toString());

				LoadYahooNewsTask task = new LoadYahooNewsTask(this, e.id);
				String apiKey = getResources().getString(R.string.yahoo_news_key);
				task.execute(e.notificationData, apiKey);

				Map<String, NotificationResult> newsUpdates = updates.get("news");
				if (!newsUpdates.containsKey(e.id))
					newsUpdates.put(e.id, e);

			} while (c.moveToNext());
		}
		c.close();
	}


	public void onLoadTweets(List<Status> tweets, String... id) {
		String eId = id[0];
		NotificationResult e = updates.get("twitter").get(eId);
		incrementUpdateCounts(e);

		if (tweets != null && !tweets.isEmpty()) {
			Log.d(TAG, "Loaded " + tweets.size() + " tweets for entity with id " + eId);

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

			Log.d(TAG, "Update last seen twitter id for entity " + e.toString());
			long ok = database.updateLastSeenNotification(e.lastSeenId, e.notificationType, e.lastSeenId);
			if (ok == 0) {
				Log.w(TAG, "Could not update last seen twitter id for entity " + e.toString());
			}
		}
	}

	public void onLoadYoutubeVideos(Video[] videos, String... id) {
		String eId = id[0];
		NotificationResult e = updates.get("youtube").get(eId);
		incrementUpdateCounts(e);

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

			Log.d(TAG, "Update last seen youtube video for entity " + e.toString());
			long ok = database.updateLastSeenNotification(e.lastSeenId, e.notificationType,
					e.lastSeenId);
			if (ok == 0) {
				Log.w(TAG, "Could not update last seen youtube video for entity " + e.toString());
			}
		}

	}

	public void onLoadYahooNews(ArrayList<NewsItem> news, String... id) {
		String eId = id[0];
		NotificationResult e = updates.get("news").get(eId);
		incrementUpdateCounts(e);

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

			Log.d(TAG, "Update last seen youtube video for entity " + e.toString());
			long ok = database.updateLastSeenNotification(e.lastSeenId, e.notificationType,
					e.lastSeenId);
			if (ok == 0) {
				Log.w(TAG, "Could not update last seen youtube video for entity " + e.toString());
			}

		}

	}
}
