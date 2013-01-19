package com.sunlightlabs.congress.services;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;


public class Congress {
	public static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static final String dateOnlyFormat = "yyyy-MM-dd";
	
	public static final String baseUrl = "http://congress.api.sunlightfoundation.com";
	
	public static final String highlightTags = "<b>,</b>"; // default highlight tags
	
	// filled in by the client
	public static String userAgent = "com.sunlightlabs.congress.services.Congress";
	public static String appVersion = null;
	public static String osVersion = null;
	public static String appChannel = null;
	public static String apiKey = "";
	
	public static final int MAX_PER_PAGE = 50;
	
	public static class SearchResult extends com.sunlightlabs.congress.models.SearchResult implements Serializable {
		private static final long serialVersionUID = 1L;
		
		static SearchResult from(JSONObject json) throws JSONException {
			SearchResult search = new SearchResult();
			
			if (!json.isNull("score"))
				search.score = json.getDouble("score");
			
			if (!json.isNull("highlight")) {
				Map<String,ArrayList<String>> highlight = new HashMap<String,ArrayList<String>>();
				
				JSONObject obj = json.getJSONObject("highlight");
				Iterator<?> iter = obj.keys();
				while (iter.hasNext()) {
					String key = (String) iter.next();
					JSONArray highlighted = obj.getJSONArray(key);
					ArrayList<String> temp = new ArrayList<String>(highlighted.length());
					for (int i=0; i<highlighted.length(); i++)
						temp.add(highlighted.getString(i));
					highlight.put(key, temp);
				}
				
				search.highlight = highlight;
			}
			
			if (!json.isNull("query"))
				search.query = json.getString("query");
			
			return search;
		}
		
	}
	
	public static String url(String method, String[] fields, Map<String,String> params) throws CongressException {
		return url(method, fields, params, -1, -1);
	}
	
	public static String url(String method, String[] fields, Map<String,String> params, int page, int per_page) throws CongressException {
		if (fields == null || fields.length == 0)
			throw new CongressException("App policy to explicitly spell out all fields.");
		
		params.put("apikey", apiKey);
		params.put("fields", TextUtils.join(",", fields));
		
		if (page > 0 && per_page > 0) {
			params.put("page", String.valueOf(page));
			params.put("per_page", String.valueOf(per_page));
		}
		
		StringBuilder query = new StringBuilder("");
		Iterator<String> iterator = params.keySet().iterator();
		
		try {
			while (iterator.hasNext()) {
				String key = iterator.next();
				String value = params.get(key);
				
				query.append(URLEncoder.encode(key, "UTF-8"));
				query.append("=");
				query.append(URLEncoder.encode(value, "UTF-8"));
				if (iterator.hasNext())
					query.append("&");
			}
		} catch(UnsupportedEncodingException e) {
			throw new CongressException(e, "Unicode not supported on this phone somehow.");
		}
		
		return baseUrl + "/" + method + "?" + query.toString();
	}
	
	public static String searchUrl(String method, String query, boolean highlight, String[] fields, Map<String,String> params, int page, int per_page) throws CongressException {
		if (highlight) {
			params.put("highlight", "true");
			if (!params.containsKey("highlight.tags"))
				params.put("highlight.tags", Congress.highlightTags);
		}
		
		params.put("query", query);
		
		return url(method + "/search", fields, params, page, per_page);
	}
	
	public static Date parseDateEither(String date) throws ParseException {
		try {
			return parseDate(date);
		} catch(ParseException e) {
			return parseDateOnly(date);
		}
	}
	
	// assumes timestamps are in UTC
	public static Date parseDate(String date) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat(dateFormat, Locale.US);
		format.setTimeZone(DateUtils.GMT);
		return format.parse(date);
	}
	
	// assumes date stamps are in "YYYY-MM-DD" format, which they will be.
	// Date objects automatically assign a time of midnight, but these dates are meant to represent whole days.
	// If we read these in as UTC, or even EST (Congress' time), then when formatted for display in the user's local timezone,
	// they could be printed as the day before the one they represent.
	// To work around Java/Android not having a class that represents a time-less day, we force the hour to be noon UTC, 
	// which means that no matter which timezone it is formatted as, it will be the same day.
	public static Date parseDateOnly(String date) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat(dateOnlyFormat, Locale.US);
		format.setTimeZone(DateUtils.GMT);
		Calendar calendar = new GregorianCalendar(DateUtils.GMT);
		calendar.setTime(format.parse(date));
		calendar.set(Calendar.HOUR_OF_DAY, 12);
		return calendar.getTime();
	}
	
	public static String formatDate(Date date) {
		return DateUtils.formatDate(date, dateFormat);
	}
	
	// format dates using the local time zone, not UTC - otherwise things start to not make sense
	public static String formatDateOnly(Date date) {
		SimpleDateFormat format = new SimpleDateFormat(dateOnlyFormat, Locale.US);
		format.setTimeZone(TimeZone.getDefault());
		return format.format(date);
	}
	
	public static String fetchJSON(String url) throws CongressException {
		Log.d(Utils.TAG, "Congress API: " + url);
		
		HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", userAgent);
        
        if (osVersion != null)
        	request.addHeader("x-os-version", osVersion);
        
        if (appVersion != null)
        	request.addHeader("x-app-version", appVersion);
        
        if (appChannel != null)
        	request.addHeader("x-app-channel", appChannel);
		
        DefaultHttpClient client = new DefaultHttpClient();
        
        try {
	        HttpResponse response = client.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();
	        
	        if (statusCode == HttpStatus.SC_OK) {
	        	String body = EntityUtils.toString(response.getEntity());
	        	return body;
	        } else if (statusCode == HttpStatus.SC_NOT_FOUND)
	        	throw new CongressException.NotFound("404 Not Found from " + url);
	        else
	        	throw new CongressException("Bad status code " + statusCode + " on fetching JSON from " + url);
        } catch (ClientProtocolException e) {
	    	throw new CongressException(e, "Problem fetching JSON from " + url);
	    } catch (IOException e) {
	    	throw new CongressException(e, "Problem fetching JSON from " + url);
	    }
	}
	
	public static JSONObject firstResult(String url) throws CongressException {
		JSONArray results = resultsFor(url);
		if (results.length() > 0) {
			try {
				return (JSONObject) results.get(0);
			} catch(JSONException e) {
				throw new CongressException(e, "Error getting first result from " + url);
			}
		} else
			return null;
	}
	
	public static JSONArray resultsFor(String url) throws CongressException {
		String rawJSON = fetchJSON(url);
		JSONArray results = null;
		try {
			results = new JSONObject(rawJSON).getJSONArray("results");
		} catch(JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
		return results;
	}

	public static List<String> listFrom(JSONArray array) throws JSONException {
		int length = array.length();
		List<String> list = new ArrayList<String>(length);
		
		for (int i=0; i<length; i++)
			list.add(array.getString(i));
		
		return list;
	}
}