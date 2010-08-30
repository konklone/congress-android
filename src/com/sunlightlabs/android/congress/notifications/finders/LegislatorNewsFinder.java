package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.LegislatorTabs;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.notifications.NotificationFinder;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.yahoo.news.NewsService;

public class LegislatorNewsFinder extends NotificationFinder {

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		try {
			String searchTerm = subscription.data;
			String apiKey = context.getResources().getString(R.string.yahoo_news_key);
			return new NewsService(apiKey).fetchNewsResults(searchTerm);
		} catch (Exception e) {
			Log.w(Utils.TAG, "LegislatorNewsFinder: Could not fetch news for legislator " + subscription.name);
			return null;
		}
	}

	@Override
	public String decodeId(Object result) {
		 if (!(result instanceof NewsItem))
			 throw new IllegalArgumentException("The result must be of type com.sunlightlabs.yahoo.news.NewsItem");
		 
		 return String.valueOf(((NewsItem) result).timestamp.toMillis(true));
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.legislatorLoadIntent(subscription.id, Utils
				.legislatorTabsIntent().putExtra("tab", LegislatorTabs.Tabs.news));
	}

}
