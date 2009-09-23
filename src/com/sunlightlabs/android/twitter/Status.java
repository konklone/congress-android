package com.sunlightlabs.android.twitter;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.Time;

public class Status {
	public String text;
	public long id;
	public Time createdAt;
	
	public Status(JSONObject json) {
		try {
			this.text = json.getString("text");
			this.id = json.getLong("id");
			this.createdAt = new Time();
			this.createdAt.set(Date.parse(json.getString("created_at")));			
		} catch (JSONException e) {
			setDefaults();
		}
	}
	
	private void setDefaults() {
		this.text = "[No tweet loaded]";
		this.createdAt = new Time();
		this.id = -1;
	}

}