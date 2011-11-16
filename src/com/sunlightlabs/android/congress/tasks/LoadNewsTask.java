package com.sunlightlabs.android.congress.tasks;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.google.news.NewsException;
import com.sunlightlabs.google.news.NewsItem;
import com.sunlightlabs.google.news.NewsService;

public class LoadNewsTask extends AsyncTask<String, Void, List<NewsItem>> {
	private CongressException exception;

	public static interface LoadsNews {
		void onLoadNews(List<NewsItem> news);
		void onLoadNews(CongressException e);
	}

	private Fragment context;

	public LoadNewsTask(Fragment context) {
		this.context = context;
	}
	
	@Override
	protected List<NewsItem> doInBackground(String... params) {
		String searchTerm = params[0];
		String apiKey = params[1];
		String referer = params[2];
		
		try {
			List<NewsItem> results = new NewsService(apiKey, referer).fetchNewsResults(searchTerm);
			Collections.sort(results, new Comparator<NewsItem>() {
				@Override
				public int compare(NewsItem a, NewsItem b) {
					return b.timestamp.compareTo(a.timestamp);
				}
			});
			return results;
		} catch (NewsException exception) {
			this.exception = new CongressException(exception, "Exception fetching news");
			return null;
		}
	}

	@Override
	protected void onPostExecute(List<NewsItem> news) {
		if (news == null && exception != null)
			((LoadsNews) context).onLoadNews(exception);
		else
			((LoadsNews) context).onLoadNews(news);
	}

}