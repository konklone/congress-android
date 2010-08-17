package com.sunlightlabs.android.congress.notifications;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.youtube.Video;
import com.sunlightlabs.youtube.YouTube;
import com.sunlightlabs.youtube.YouTubeException;

public class YoutubeResultProcessor extends NotificationChecker {

	public YoutubeResultProcessor(Context context) {
		super(context);
	}

	@Override
	public String decodeId(Object result) {
		if (!(result instanceof Video))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.youtube.Video");
		return String.valueOf(((Video) result).timestamp.toMillis(true));
	}

	@Override
	public List<?> callUpdate(String data) {
		try {
			return Arrays.asList(new YouTube().getVideos(data));
		} catch (YouTubeException e) {
			Log.w(Utils.TAG, "YoutubeResultProcessor: Could not fetch youtube videos for " + data, e);
			return null;
		}
	}
}
