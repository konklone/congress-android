package com.sunlightlabs.google.news;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NewsService {
	public static final String BASE_URL = "https://ajax.googleapis.com/ajax/services/search/news";
	public static final String USER_AGENT = "Sunlight's Congress Android App (http://github.com/sunlightlabs/congress)";
	
	public String apiKey;
	// Google requires a valid referer, that is within the domain/path of the URL that the API key is registered under.
	public String referer; 
	
	public String scoring; // order by 
	public int rsz; // number of results
	
	
	public NewsService(String apiKey, String referer) {
		this.scoring = "d"; // date
		this.rsz = 8; // maximum number of results
		
		this.apiKey = apiKey;
		this.referer = referer;
	}
	
	public ArrayList<NewsItem> fetchNewsResults(String query) throws NewsException {
		String rawJSON = fetchJSON(query);
		ArrayList<NewsItem> items;
		try {
			JSONObject resultSet = new JSONObject(rawJSON);
			JSONArray results = resultSet.getJSONObject("responseData").getJSONArray("results");
			items = new ArrayList<NewsItem>(results.length());
			for (int i = 0; i<results.length(); i++)
				items.add(new NewsItem(results.getJSONObject(i)));
				
		} catch(JSONException e) {
			throw new NewsException(e);
		}
		
		return items;
	}
	
	public String fetchJSON(String query) throws NewsException {
		String queryString = queryString(query);
		
		String url = BASE_URL + "?" + queryString;
		HttpGet request = new HttpGet(url);
		
		request.addHeader("User-Agent", USER_AGENT);
        request.addHeader("Referer", referer);
		
        DefaultHttpClient client = new DefaultHttpClient();
        
        try {
	        HttpResponse response = client.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();
	        
	        if (statusCode == HttpStatus.SC_OK) {
	        	String body = EntityUtils.toString(response.getEntity());
	        	return body;
	        } else {
	        	throw new NewsException("Bad status code on fetching news", statusCode);
	        }
        } catch (Exception e) {
	    	throw new NewsException(e);
	    }
	}
	
	private String queryString(String query) {
		StringBuffer sb = new StringBuffer();
		
		sb.append(queryPair("rsz", "" + rsz));
		sb.append("&");
		sb.append(queryPair("scoring", scoring));
		sb.append("&");
		
		sb.append(queryPair("v", "1.0"));
		sb.append("&");
		sb.append(queryPair("key", apiKey));
		sb.append("&");
		
		sb.append(queryPair("q", "\"" + query + "\""));
		
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