package com.sunlightlabs.google.news;

import java.util.Date;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class NewsItem {
	public String title, source, clickURL, summary;
	public Date timestamp;
	
	public static final String[] dateFormat = new String[] {"EEE, dd MMM yyyy HH:mm:ss Z"};
	
	public NewsItem(JSONObject json) throws JSONException {
		this.title = unescapeHtml(json.getString("titleNoFormatting"));
		this.clickURL = json.getString("unescapedUrl");
		this.source = json.getString("publisher");
		this.summary = json.getString("content");
		
		String publishedDate = json.getString("publishedDate");
		try {
			this.timestamp = DateUtils.parseDate(publishedDate, dateFormat);
		} catch(DateParseException e) {
			throw new JSONException("Couldn't parse date on news item.");
		}
	}
	
	// very very minimal unescaping of common characters, not a robust thing
	public static String unescapeHtml(String s) {
		return s.replace("&quot;", "\"").replace("&#39;", "'");
	}
}