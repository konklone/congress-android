package com.sunlightlabs.android.congress.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

public class ShortcutImageTask extends AsyncTask<String,Void,Bitmap> {
	public CreatesShortcutImage context;
	
	public ShortcutImageTask(CreatesShortcutImage context) {
		super();
		this.context = context;
	}
	
	public void onScreenLoad(CreatesShortcutImage context) {
		this.context = context;
	}
	
	@Override
	protected Bitmap doInBackground(String... bioguideId) {
		return LegislatorImage.shortcutImage(bioguideId[0], context.getContext());
	}
	
	@Override
	protected void onPostExecute(Bitmap shortcutIcon) {
		context.onCreateShortcutIcon(shortcutIcon);
	}
	
	public interface CreatesShortcutImage {
		public void onCreateShortcutIcon(Bitmap shortcutIcon);
		public Context getContext();
	}
}