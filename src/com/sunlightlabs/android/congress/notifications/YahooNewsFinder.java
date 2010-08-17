package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.yahoo.news.NewsException;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.yahoo.news.NewsService;

public class YahooNewsFinder extends NotificationFinder {

	public YahooNewsFinder(Context context) {
		super(context);
	}

	@Override
	public String decodeId(Object result) {
		if (!(result instanceof NewsItem))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.yahoo.news.NewsItem");
		return String.valueOf(((NewsItem) result).timestamp.toMillis(true));
	}

	@Override
	public List<?> callUpdate(String data) {
		try {
			return new NewsService(context.getResources().getString(R.string.yahoo_news_key))
					.fetchNewsResults(data);
		} catch (NotFoundException e) {
			Log.w(Utils.TAG, "Could not fetch yahoo news for " + data, e);
			return null;
		} catch (NewsException e) {
			Log.w(Utils.TAG, "Could not fetch yahoo news for " + data, e);
			return null;
		}
	}
}
