package com.sunlightlabs.android.congress.tasks;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

import com.sunlightlabs.google.news.NewsException;
import com.sunlightlabs.google.news.NewsItem;
import com.sunlightlabs.google.news.NewsService;

public class LoadNewsTask extends AsyncTask<String, Void, ArrayList<NewsItem>> {
	private final static String TAG = "CONGRESS";

	public static interface LoadsNews {
		void onLoadNews(ArrayList<NewsItem> news);
	}

	private LoadsNews context;

	public LoadNewsTask(LoadsNews context) {
		super();
		this.context = context;
	}

	public void onScreenLoad(LoadsNews context) {
		this.context = context;
	}

	@Override
	protected ArrayList<NewsItem> doInBackground(String... params) {
		if(params == null || params.length < 2) {
			Log.w(TAG, "Could not fetch news. "
					+ "Parameters must contain api key and search term.");
			return null;
		}
		
		String searchTerm = params[0];
		String apiKey = params[1];
		String referer = params[2];
		try {
			return new NewsService(apiKey, referer).fetchNewsResults(searchTerm);
		} catch (NewsException e) {
			Log.w(TAG, "Could not fetch news for search term " + searchTerm);
			return null;
		}
	}

	@Override
	protected void onPostExecute(ArrayList<NewsItem> news) {
		context.onLoadNews(news);
	}

}
