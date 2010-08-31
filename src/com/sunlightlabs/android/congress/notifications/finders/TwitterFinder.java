package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.Twitter.Status;
import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.NotificationFinder;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;

public class TwitterFinder extends NotificationFinder {

	@Override
	public String decodeId(Object result) {
		return String.valueOf(((Status) result).id);
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		String username = subscription.data;
		
		try {
			return new Twitter().getUserTimeline(username);
		} catch (TwitterException exc) {
			Log.w(Utils.TAG, "Could not fetch tweets for " + subscription, exc);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results > 1)
			return results + " new tweets.";
		else
			return results + " new tweet.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.legislatorLoadIntent(subscription.id, Utils
				.legislatorTabsIntent().putExtra("tab", "tweets"));
	}
}