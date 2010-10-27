package com.sunlightlabs.youtube;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.Time;

public class Video {
	public String title, description, url, thumbnailUrl;
	public Time timestamp;
	
	public Video(JSONObject json) throws JSONException {
		this.title = json.getString("title");
		this.description = json.getString("description");
		this.url = json.getJSONObject("player").getString("default");
		this.thumbnailUrl = json.getJSONObject("thumbnail").getString("sqDefault");
		
		String updated = json.getString("updated");
		this.timestamp = new Time();
		this.timestamp.parse3339(updated);
	}
	
}