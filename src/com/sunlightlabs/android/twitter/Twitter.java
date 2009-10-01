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
	
	
	public Twitter() {
		this.page = 1;
		this.count = 20;
		this.type = "json";
	}
	
	
	public Status[] getUserTimeline(String username) throws TwitterException {
		String rawJSON = fetchJSON(userTimelineUrl(username));
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
	
	private String userTimelineUrl(String username) {
		return url("/statuses/user_timeline/" + username);
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
