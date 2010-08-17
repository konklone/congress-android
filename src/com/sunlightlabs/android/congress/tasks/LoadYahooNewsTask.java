package com.sunlightlabs.android.congress.tasks;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

import com.sunlightlabs.yahoo.news.NewsException;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.yahoo.news.NewsService;

public class LoadYahooNewsTask extends AsyncTask<String, Void, ArrayList<NewsItem>> {
	private final static String TAG = "CONGRESS";

	public static interface LoadsYahooNews {
		void onLoadYahooNews(ArrayList<NewsItem> news);
	}

	private LoadsYahooNews context;

	public LoadYahooNewsTask(LoadsYahooNews context) {
		super();
		this.context = context;
	}

	public void onScreenLoad(LoadsYahooNews context) {
		this.context = context;
	}

	@Override
	protected ArrayList<NewsItem> doInBackground(String... params) {
		if(params == null || params.length < 2) {
			Log.w(TAG, "Could not fetch yahoo news. "
					+ "Parameters must contain api key and search term.");
			return null;
		}
		
		String searchTerm = params[0];
		String apiKey = params[1];
		try {
			return new NewsService(apiKey).fetchNewsResults(searchTerm);
		} catch (NewsException e) {
			Log.w(TAG, "Could not fetch yahoo news for search term " + searchTerm);
			return null;
		}
	}

	@Override
	protected void onPostExecute(ArrayList<NewsItem> news) {
		context.onLoadYahooNews(news);
	}

}
