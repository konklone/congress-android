package com.sunlightlabs.android.congress.tasks;

import java.util.List;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.os.AsyncTask;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;

public class LoadTweetsTask extends AsyncTask<String, Void, List<Twitter.Status>> {
	public static interface LoadsTweets {
		void onLoadTweets(List<Twitter.Status> tweets);
	}

	private LoadsTweets context;

	public LoadTweetsTask(LoadsTweets context) {
		super();
		this.context = context;
	}


	public void onScreenLoad(LoadsTweets context) {
		this.context = context;
	}

	@Override
	protected List<Twitter.Status> doInBackground(String... username) {
		try {
			return new Twitter().getUserTimeline(username[0]);
		} catch (TwitterException e) {
			Log.w(Utils.TAG, "Couldn't get twitter timeline for " + username[0]);
			return null;
		}
	}

	@Override
	protected void onPostExecute(List<Twitter.Status> tweets) {
		context.onLoadTweets(tweets);
	}

}
