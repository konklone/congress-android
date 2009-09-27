package com.sunlightlabs.android.twitter;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.Time;

public class Status {
	public long id;
	public String text;
	public long createdAtMillis;
	public Time createdAt;
	
	public Status(JSONObject json) {
		try {
			this.text = json.getString("text");
			this.id = json.getLong("id");
			this.createdAt = new Time();
			this.createdAtMillis = Date.parse(json.getString("created_at"));
			this.createdAt.set(this.createdAtMillis);			
		} catch (JSONException e) {
			setDefaults();
		}
	}
	
	private void setDefaults() {
		this.text = "[No tweet loaded]";
		this.createdAtMillis = System.currentTimeMillis();
		this.createdAt = new Time();
		this.createdAt.set(this.createdAtMillis);
		this.id = -1;
	}

}