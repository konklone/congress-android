package com.sunlightlabs.google.news;

import android.util.Log;

import com.sunlightlabs.android.congress.utils.HttpManager;
import com.sunlightlabs.android.congress.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class NewsService {
	public static final int RESULTS = 8;
	
	public static final String BASE_URL = "https://ajax.googleapis.com/ajax/services/search/news";
	public static final String USER_AGENT = "Sunlight's Congress Android App (https://github.com/sunlightlabs/congress-android)";
	
	public String apiKey;
	// Google requires a valid referer, that is within the domain/path of the URL that the API key is registered under.
	public String referer; 
	
	public String ned; // news edition (country)
	
	
	public NewsService(String apiKey, String referer) {
		this.ned = "us"; // US edition
		
		this.apiKey = apiKey;
		this.referer = referer;
	}
	
	public List<NewsItem> fetchNewsResults(String query) throws NewsException {
		String rawJSON = fetchJSON(query);
		List<NewsItem> items;
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

        Log.d(Utils.TAG, "Google News: " + url);

        // play nice with OkHttp
        HttpManager.init();

        HttpURLConnection connection;
        URL theUrl;

        try {
            theUrl = new URL(url);
            connection = (HttpURLConnection) theUrl.openConnection();
        } catch(MalformedURLException e) {
            throw new NewsException(e);
        } catch (IOException e) {
            throw new NewsException(e);
        }

        try {
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Referer", referer);

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                // read input stream first to fetch response headers
                InputStream in = connection.getInputStream();

                // adapted from http://stackoverflow.com/a/2549222/16075
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) total.append(line);

                return total.toString();

            } else
                throw new NewsException("Error talking to Google News.", status);
        } catch (IOException e) {
            throw new NewsException(e);
        } finally {
            connection.disconnect();
        }
	}
	
	private String queryString(String query) {
		StringBuffer sb = new StringBuffer();
		
		sb.append(queryPair("rsz", "" + RESULTS));
		sb.append("&");
		sb.append(queryPair("ned", ned));
		sb.append("&");
		sb.append(queryPair("v", "1.0"));
		sb.append("&");
		sb.append(queryPair("key", apiKey));
		sb.append("&");
		
		sb.append(queryPair("q", query));
		
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