package com.sunlightlabs.android.congress.tasks;

import com.sunlightlabs.android.congress.utils.ImageUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

public class LoadYoutubeThumbTask extends AsyncTask<String, Void, Drawable> {
	private LoadsThumb context;
	public String size;
	public Object tag = null;

	public LoadYoutubeThumbTask(LoadsThumb context, String size) {
		this.context = context;
		this.size = size;
	}

	public LoadYoutubeThumbTask(LoadsThumb context, String size, Object tag) {
		this(context, size);
		this.tag = tag;
	}

	public void onScreenLoad(LoadsThumb context) {
		this.context = context;
	}
	@Override
	protected Drawable doInBackground(String... url) {
		return ImageUtils.getImage(ImageUtils.YOUTUBE_THUMB, url[0], context.getContext());
	}

	@Override
	protected void onPostExecute(Drawable result) {
		context.onLoadThumb(result, tag);
	}

	public interface LoadsThumb {
		public void onLoadThumb(Drawable thumb, Object tag);

		public Context getContext();
	}
}