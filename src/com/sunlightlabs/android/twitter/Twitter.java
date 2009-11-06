package com.sunlightlabs.android.twitter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Twitter {
	private static final String USER_AGENT = "Sunlight's Congress Android App (http://github.com/sunlightlabs/congress";
	public static final String BASE_URL = "twitter.com";
	
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
	public boolean update(String message) throws TwitterException {
		// final sanity check before we make a network request
		if (this.username == null || this.password == null)
			return false;
		
		HttpPost request = new HttpPost(updateUrl());
		request.addHeader("User-Agent", USER_AGENT);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("status", message));
		
		// must turn off the expects continue header or else Twitter replies with a 417
		HttpParams http = new BasicHttpParams();
		HttpProtocolParams.setUseExpectContinue(http, false);
		
		// set auth credentials
		Credentials credentials = new UsernamePasswordCredentials(username, password);
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(BASE_URL, 80), credentials);
		
		DefaultHttpClient client = new DefaultHttpClient(http);
		client.setCredentialsProvider(credsProvider);
		
		try {
			request.setEntity(new UrlEncodedFormEntity(params));
    		HttpResponse response = client.execute(request);
    		String body = EntityUtils.toString(response.getEntity());
    		return !(new JSONObject(body).has("error"));
		} catch(Exception e) {
        	throw new TwitterException(e);
        }
	}
	
	public String fetchJSON(String url) throws TwitterException {
		HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", USER_AGENT);
		
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
		return "http://" + BASE_URL + "/statuses/update." + type;
	}
	
	private String url(String path) {
		return "http://" + BASE_URL + path + "." + type + "?" + queryString();	
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
