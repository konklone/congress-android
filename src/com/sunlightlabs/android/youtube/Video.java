package com.sunlightlabs.android.youtube;

import android.text.format.Time;

public class Video {
	public String title, description, url;
	public Time timestamp;
	
	public Video(String xml) {
		this.title = "title";
		this.description = "description";
		this.timestamp = new Time();
		this.timestamp.set(System.currentTimeMillis());
	}
	
}
