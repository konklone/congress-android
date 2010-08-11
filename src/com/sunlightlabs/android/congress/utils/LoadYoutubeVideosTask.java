package com.sunlightlabs.android.congress.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.sunlightlabs.youtube.Video;
import com.sunlightlabs.youtube.YouTube;
import com.sunlightlabs.youtube.YouTubeException;

public class LoadYoutubeVideosTask extends AsyncTask<String, Void, Video[]> {
	private final static String TAG = "CONGRESS";

	public static interface LoadsYoutubeVideos {
		void onLoadYoutubeVideos(Video[] videos, String... id);
	}

	private LoadsYoutubeVideos context;
	private String id;

	public LoadYoutubeVideosTask(LoadsYoutubeVideos context, String id) {
		super();
		this.context = context;
		this.id = id;
	}

	public LoadYoutubeVideosTask(LoadsYoutubeVideos context) {
		this(context, null);
	}

	public void onScreenLoad(LoadsYoutubeVideos context) {
		this.context = context;
	}

	@Override
	protected Video[] doInBackground(String... username) {
		try {
			return new YouTube().getVideos(username[0]);
		} catch (YouTubeException e) {
			Log.w(TAG, "Could not load youtube videos for " + username[0]);
			return null;
		}
	}

	@Override
	protected void onPostExecute(Video[] videos) {
		if (id != null)
			context.onLoadYoutubeVideos(videos, id);
		else
			context.onLoadYoutubeVideos(videos);
	}
}