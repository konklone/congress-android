package com.sunlightlabs.android.yahoo.news;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

public class NewsService {
	public static final String BASE_URL = "http://search.yahooapis.com/NewsSearchService/V1/newsSearch";
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
	
	public String fetchNewsResults(String query) throws NewsException {
		String queryString = queryString(query);
		String url = BASE_URL + "?" + queryString;
		HttpGet request = new HttpGet(url);
        //request.addHeader("User-Agent", "Sunlight's Congress Android App (http://github.com/sunlightlabs/congress");
		
        DefaultHttpClient client = new DefaultHttpClient();
        
        try {
	        HttpResponse response = client.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();
	        String body = EntityUtils.toString(response.getEntity());
	        
	        if (statusCode == HttpStatus.SC_OK) {
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