package com.sunlightlabs.android.congress.notifications.finders;

import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.NotificationEntity;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.youtube.Video;
import com.sunlightlabs.youtube.YouTube;
import com.sunlightlabs.youtube.YouTubeException;

public class YoutubeFinder extends NotificationFinder {

	@Override
	public String decodeId(Object result) {
		if (!(result instanceof Video))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.youtube.Video");
		return String.valueOf(((Video) result).timestamp.toMillis(true));
	}

	@Override
	public List<?> callUpdate(NotificationEntity entity) {
		try {
			return Arrays.asList(new YouTube().getVideos(entity.notificationData));
		} catch (YouTubeException e) {
			Log.w(Utils.TAG, "YoutubeResultProcessor: Could not fetch youtube videos for " + entity, e);
			return null;
		}
	}

	@Override
	public Intent notificationIntent(NotificationEntity entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String notificationMessage(NotificationEntity entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String notificationTitle(NotificationEntity entity) {
		// TODO Auto-generated method stub
		return null;
	}
}
