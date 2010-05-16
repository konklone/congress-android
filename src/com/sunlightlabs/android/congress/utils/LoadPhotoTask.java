package com.sunlightlabs.android.congress.utils;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

public class LoadPhotoTask extends AsyncTask<String,Void,Drawable> {
	public LoadsPhoto context;
	public String size;
	
	public LoadPhotoTask(LoadsPhoto context, String size) {
		super();
		this.context = context;
		this.size = size;
	}
	
	public void onScreenLoad(LoadsPhoto context) {
		this.context = context;
	}
	
	@Override
	public Drawable doInBackground(String... bioguideId) {
		return LegislatorImage.getImage(size, bioguideId[0], context.photoContext());
	}
	
	@Override
	public void onPostExecute(Drawable photo) {
		context.onLoadPhoto(photo);
	}
}