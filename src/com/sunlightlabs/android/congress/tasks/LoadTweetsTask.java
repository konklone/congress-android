package com.sunlightlabs.android.congress.tasks;

import java.util.List;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.os.AsyncTask;

public class LoadTweetsTask extends AsyncTask<String, Void, List<Twitter.Status>> {
	public static interface LoadsTweets {
		void onLoadTweets(List<Twitter.Status> tweets);
		void onLoadTweets(TwitterException e);
	}
	
	private TwitterException exception;
	private LoadsTweets context;

	public LoadTweetsTask(LoadsTweets context) {
		super();
		this.context = context;
	}

	@Override
	protected List<Twitter.Status> doInBackground(String... username) {
		try {
			return new Twitter().getUserTimeline(username[0]);
		} catch (TwitterException e) {
			this.exception = e;
			return null;
		}
	}

	@Override
	protected void onPostExecute(List<Twitter.Status> tweets) {
		if (tweets != null && exception == null)
			context.onLoadTweets(tweets);
		else
			context.onLoadTweets(exception);
	}

}
