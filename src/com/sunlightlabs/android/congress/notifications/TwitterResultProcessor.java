package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.Twitter.Status;
import android.content.Context;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;

public class TwitterResultProcessor extends NotificationChecker {

	public TwitterResultProcessor(Context context) {
		super(context);
	}

	@Override
	public String decodeId(Object result) {
		if (!(result instanceof Status))
			throw new IllegalArgumentException("The result must be of type winterwell.jtwitter.Twitter.Status");
		return String.valueOf(((Status) result).id);
	}

	@Override
	public List<?> callUpdate(String data) {
		try {
			return new Twitter().getUserTimeline(data);
		} catch (TwitterException exc) {
			Log.w(Utils.TAG, "Could not fetch tweets for " + data, exc);
			return null;
		}
	}

}
