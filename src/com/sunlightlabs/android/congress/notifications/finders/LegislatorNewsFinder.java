package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.LegislatorTabs;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.NotificationEntity;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.yahoo.news.NewsService;

public class LegislatorNewsFinder extends NotificationFinder {

	@Override
	public List<?> callUpdate(NotificationEntity entity) {
		try {
			return new NewsService(context.getResources().getString(R.string.yahoo_news_key))
					.fetchNewsResults(entity.notificationData);
		} catch (Exception e) {
			Log.w(Utils.TAG, "Could not fetch yahoo news for " + entity.id);
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
	public Intent notificationIntent(NotificationEntity entity) {
		return Utils.legislatorLoadIntent(entity.id, Utils
				.legislatorTabsIntent().putExtra("tab", LegislatorTabs.Tabs.news));
	}

}
