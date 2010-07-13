package com.sunlightlabs.yahoo.news;

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
	public static final String BASE_URL = "http://search.yahooapis.com/NewsSearchService/V1/newsSearch";
	public static final String USER_AGENT = "Sunlight's Congress Android App (http://github.com/sunlightlabs/congress)";
	public String type, sort, language, output;
	public int results;
	
	public String apiKey;
	
	
	public NewsService(String apiKey) {
		this.type = "phrase";
		this.sort = "date";
		this.language = "en";
		this.output = "json";
		this.results = 10;
		
		this.apiKey = apiKey;
	}
	
	public ArrayList<NewsItem> fetchNewsResults(String query) throws NewsException {
		String rawJSON = fetchJSON(query);
		ArrayList<NewsItem> items;
		try {
			JSONObject resultSet = new JSONObject(rawJSON);
			JSONArray results = resultSet.getJSONObject("ResultSet").getJSONArray("Result");
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
		
		sb.append(queryPair("type", type));
		sb.append("&");
		sb.append(queryPair("sort", sort));
		sb.append("&");
		sb.append(queryPair("language", language));
		sb.append("&");
		sb.append(queryPair("output", output));
		sb.append("&");
		sb.append(queryPair("results", "" + results));
		sb.append("&");
		sb.append(queryPair("appid", apiKey));
		sb.append("&");
		
		sb.append(queryPair("query", query));
		
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