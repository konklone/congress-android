package com.sunlightlabs.android.congress.notifications;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.Twitter.Status;
import android.content.Context;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;

public class TwitterResultProcessor extends ResultProcessor {

	public TwitterResultProcessor(Context context, NotificationEntity entity) {
		super(context, entity);
	}

	@Override
	public String decodeId(Object result) {
		if (!(result instanceof Status))
			throw new IllegalArgumentException("The result must be of type winterwell.jtwitter.Twitter.Status");
		return String.valueOf(((Status) result).id);
	}

	@Override
	public void callUpdate() {
		try {
			processResults(new Twitter().getUserTimeline(entity.notification_data));
		} catch (TwitterException exc) {
			Log.w(Utils.TAG, "Could not fetch tweets for " + entity.id + " using "
					+ entity.notification_data, exc);
		}
	}

}
