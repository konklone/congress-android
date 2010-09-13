package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.yahoo.news.NewsService;

public class NewsBillSubscriber extends Subscriber {

	@Override
	public String decodeId(Object result) {
		return "" + ((NewsItem) result).timestamp.toMillis(false);
	}
	
	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		String searchTerm = subscription.data;
		String apiKey = context.getResources().getString(R.string.yahoo_news_key);
		
		try {
			return new NewsService(apiKey).fetchNewsResults(searchTerm);
		} catch (Exception e) {
			Log.w(Utils.TAG, "NewsLegislatorSubscriber: Could not fetch news for legislator " + subscription.name);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results > 1)
			return results + " new mentions in the news.";
		else
			return results + " new mention in the news.";
	}
	
	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.billLoadIntent(subscription.id, 
				Utils.billTabsIntent().putExtra("tab", "news"));
	}
}