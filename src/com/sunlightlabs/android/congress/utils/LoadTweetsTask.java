package com.sunlightlabs.android.congress.utils;

import java.util.List;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.os.AsyncTask;
import android.util.Log;

public class LoadTweetsTask extends AsyncTask<String, Void, List<Twitter.Status>> {
	private final static String TAG = "CONGRESS";

	public static interface LoadsTweets {
		void onLoadTweets(List<Twitter.Status> tweets, String... id);
	}

	private LoadsTweets context;
	private String id; // the id of the task

	public LoadTweetsTask(LoadsTweets context, String id) {
		super();
		this.context = context;
		this.id = id;
	}

	public LoadTweetsTask(LoadsTweets context) {
		this(context, null);
	}

	public void onScreenLoad(LoadsTweets context) {
		this.context = context;
	}

	public String getId() {
		return id;
	}

	@Override
	protected List<Twitter.Status> doInBackground(String... username) {
		try {
			return new Twitter().getUserTimeline(username[0]);
		} catch (TwitterException e) {
			Log.w(TAG, "Couldn't get twitter timeline for " + username[0]);
			return null;
		}
	}

	@Override
	protected void onPostExecute(List<Twitter.Status> tweets) {
		if (id != null)
			context.onLoadTweets(tweets, id);
		else
			context.onLoadTweets(tweets);
	}

}
