package com.sunlightlabs.android.congress.notifications;

import java.util.Arrays;

import android.content.Context;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.youtube.Video;
import com.sunlightlabs.youtube.YouTube;
import com.sunlightlabs.youtube.YouTubeException;

public class YoutubeResultProcessor extends ResultProcessor {

	public YoutubeResultProcessor(Context context, NotificationEntity entity) {
		super(context, entity);
	}

	@Override
	public String decodeId(Object result) {
		if (!(result instanceof Video))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.youtube.Video");
		return String.valueOf(((Video) result).timestamp.toMillis(true));
	}

	@Override
	public void callUpdate() {
		try {
			processResults(Arrays.asList(new YouTube().getVideos(entity.notification_data)));
		} catch (YouTubeException e) {
			Log.w(Utils.TAG, "YoutubeResultProcessor: Could not fetch youtube videos for "
					+ entity.id + " using " + entity.notification_data, e);
		}
	}
}
