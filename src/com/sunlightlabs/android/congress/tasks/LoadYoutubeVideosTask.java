package com.sunlightlabs.android.congress.tasks;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

import com.sunlightlabs.youtube.Video;
import com.sunlightlabs.youtube.YouTube;
import com.sunlightlabs.youtube.YouTubeException;

public class LoadYoutubeVideosTask extends AsyncTask<String, Void, ArrayList<Video>> {
	private final static String TAG = "CONGRESS";

	public static interface LoadsYoutubeVideos {
		void onLoadYoutubeVideos(ArrayList<Video> videos);
	}

	private LoadsYoutubeVideos context;

	public LoadYoutubeVideosTask(LoadsYoutubeVideos context) {
		super();
		this.context = context;
	}

	public void onScreenLoad(LoadsYoutubeVideos context) {
		this.context = context;
	}

	@Override
	protected ArrayList<Video> doInBackground(String... username) {
		try {
			return new YouTube().getVideos(username[0]);
		} catch (YouTubeException e) {
			Log.w(TAG, "Could not load youtube videos for " + username[0]);
			return null;
		}
	}

	@Override
	protected void onPostExecute(ArrayList<Video> videos) {
		context.onLoadYoutubeVideos(videos);
	}
}