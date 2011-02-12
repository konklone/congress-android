package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.google.news.NewsItem;
import com.sunlightlabs.google.news.NewsService;

public class NewsLegislatorSubscriber extends Subscriber {
	
	@Override
	public String decodeId(Object result) {
		return "" + ((NewsItem) result).timestamp.getTime();
	}
	
	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		String searchTerm = subscription.data;
		String apiKey = context.getResources().getString(R.string.google_news_key);
		String referer = context.getResources().getString(R.string.google_news_referer);
		
		try {
			return new NewsService(apiKey, referer).fetchNewsResults(searchTerm);
		} catch (Exception e) {
			Log.w(Utils.TAG, "NewsLegislatorSubscriber: Could not fetch news for legislator " + subscription.name);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results == NewsService.RESULTS)
			return results + " or more new mentions in the news.";
		else if (results > 1)
			return results + " new mentions in the news.";
		else
			return results + " new mention in the news.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.legislatorLoadIntent(subscription.id, 
				Utils.legislatorTabsIntent().putExtra("tab", "news"));
	}
}