package com.sunlightlabs.android.twitter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.text.format.Time;


public class Twitter {
	public static final String BASE_URL = "http://twitter.com";
	
	public int page, count;
	public String type;
	
	public String username, password;
	
	
	public Twitter() {
		this.page = 1;
		this.count = 20;
		this.type = "json";
		this.username = null;
		this.password = null;
	}
	
	public Twitter(String username, String password) {
		this.page = 1;
		this.count = 20;
		this.type = "json";
		this.username = username;
		this.password = password;
	}
	
	public Status[] getUserTimeline(String userId) throws TwitterException {
		String rawJSON = fetchJSON(userTimelineUrl(userId));
		Status[] tweets;
		try {
			
			JSONArray rawTweets = new JSONArray(rawJSON);
			tweets = new Status[rawTweets.length()];
			for (int i = 0; i<rawTweets.length(); i++)
				tweets[i] = new Status(rawTweets.getJSONObject(i));
			
		} catch(JSONException e) {
			throw new TwitterException(e);
		}
		
		return tweets;
	}
	
	/** 
	 * Updates one's Twitter account.
	 */
	public boolean update(String message) {
		// final sanity check before we make a network request
		if (this.username == null || this.password == null)
			return false;
		
		return true;
	}
	
	public String fetchJSON(String url) throws TwitterException {
		HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", "Sunlight's Congress Android App (http://github.com/sunlightlabs/congress");
		
        DefaultHttpClient client = new DefaultHttpClient();
        
        try {
	        HttpResponse response = client.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();
	        
	        if (statusCode == HttpStatus.SC_OK) {
	        	String body = EntityUtils.toString(response.getEntity());
	        	return body;
	        } else {
	        	throw new TwitterException("Bad status code on fetching tweets: " + statusCode);
	        }
        } catch (Exception e) {
	    	throw new TwitterException(e);
	    }
	}
	
	private String userTimelineUrl(String userId) {
		return url("/statuses/user_timeline/" + userId);
	}
	
	private String updateUrl() {
		return BASE_URL + "/statuses/update." + type;
	}
	
	private String url(String path) {
		return BASE_URL + path + "." + type + "?" + queryString();	
	}
	
	private String queryString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(queryPair("page", "" + page));
		sb.append("&");
		sb.append(queryPair("count", "" + count));
		
		return sb.toString();
	}
	
	private String queryPair(String name, String value) {
		try {
			return URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
		} catch(UnsupportedEncodingException neverHappen) {
			return name + "=" + value;
		}
	}
}
