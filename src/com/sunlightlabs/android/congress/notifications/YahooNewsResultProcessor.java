package com.sunlightlabs.android.congress.notifications;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.yahoo.news.NewsException;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.yahoo.news.NewsService;

public class YahooNewsResultProcessor extends ResultProcessor {

	public YahooNewsResultProcessor(Context context, NotificationEntity entity) {
		super(context, entity);
	}

	@Override
	public String decodeId(Object result) {
		if (!(result instanceof NewsItem))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.yahoo.news.NewsItem");
		return String.valueOf(((NewsItem) result).timestamp.toMillis(true));
	}

	@Override
	public void callUpdate() {
		try {
			processResults(new NewsService(context.getResources().getString(R.string.yahoo_news_key))
					.fetchNewsResults(entity.notification_data));
		} catch (NotFoundException e) {
			Log.w(Utils.TAG, "Could not fetch yahoo news for " + entity.id + " using "
					+ entity.notification_data, e);
		} catch (NewsException e) {
			Log.w(Utils.TAG, "Could not fetch yahoo news for " + entity.id + " using "
					+ entity.notification_data, e);
		}
	}
}
