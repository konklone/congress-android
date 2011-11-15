package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.youtube.Video;
import com.sunlightlabs.youtube.YouTube;
import com.sunlightlabs.youtube.YouTubeException;

public class YoutubeSubscriber extends Subscriber {
	public static final int PER_PAGE = 25; // what YouTube returns by default
	
	@Override
	public String decodeId(Object result) {
		return "" + ((Video) result).timestamp;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		String username = subscription.data;
		
		try {
			return new YouTube().getVideos(username);
		} catch (YouTubeException e) {
			Log.w(Utils.TAG, "YoutubeSubscriber: Could not fetch youtube videos for " + subscription, e);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results == PER_PAGE)
			return results + " or more new videos.";
		else if (results > 1)
			return results + " new videos.";
		else
			return results + " new video.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.legislatorLoadIntent(subscription.id, Utils
				.legislatorPagerIntent().putExtra("tab", "videos"));
	}
	
	@Override
	public String subscriptionName(Subscription subscription) {
		return "Videos: " + subscription.name;
	}
	
	@Override
	public int subscriptionIcon(Subscription subscription) {
		return R.drawable.person;
	}
}