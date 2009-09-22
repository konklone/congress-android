package com.sunlightlabs.android.yahoo.news;

import org.json.JSONObject;

import android.text.format.Time;

public class NewsItem {
	public String title, source, displayURL, clickURL;
	public Time timestamp;
	
	public NewsItem() {}
	
	public NewsItem(String title, String source, String displayURL, String clickURL, Time timestamp) {
		this.title = title;
		this.displayURL = displayURL;
		this.clickURL = clickURL;
		this.source = source;
		this.timestamp = timestamp;
	}
	
	public NewsItem(JSONObject json) {
		
	}
}