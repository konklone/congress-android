package com.sunlightlabs.android.congress.tasks;

import com.sunlightlabs.android.congress.utils.LegislatorImage;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

public class LoadPhotoTask extends AsyncTask<String,Void,Drawable> {
	public LoadsPhoto context;
	public String size;
	public Object tag = null;
	
	public LoadPhotoTask(LoadsPhoto context, String size) {
		super();
		this.context = context;
		this.size = size;
	}
	
	public LoadPhotoTask(LoadsPhoto context, String size, Object tag) {
		super();
		this.context = context;
		this.size = size;
		this.tag = tag;
	}
	
	public void onScreenLoad(LoadsPhoto context) {
		this.context = context;
	}
	
	@Override
	public Drawable doInBackground(String... bioguideId) {
		return LegislatorImage.getImage(size, bioguideId[0], context.getContext());
	}
	
	@Override
	public void onPostExecute(Drawable photo) {
		context.onLoadPhoto(photo, tag);
	}
	
	public interface LoadsPhoto {
		public void onLoadPhoto(Drawable photo, Object tag);
		public Context getContext();
	}
}