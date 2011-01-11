package com.sunlightlabs.congress.services;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.util.EntityUtils;

import android.text.TextUtils;

import com.sunlightlabs.congress.models.CongressException;


public class RealTimeCongress {
	public static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	public static final String baseUrl = "http://api.realtimecongress.org/api/v1/";
	public static final String format = "json";
	
	// filled in by the client
	public static String userAgent = "com.sunlightlabs.congress.services.RealTimeCongress";
	public static String appVersion = "unversioned";
	public static String apiKey = "";
	
	// optional, if you want to add a custom header/value for your environment
	public static String extraHeaderKey = null;
	public static String extraHeaderValue = null;
	
	
	public static String url(String method, String[] sections, Map<String,String> params) {
		params.put("apikey", apiKey);
		params.put("sections", TextUtils.join(",", sections));
		
		StringBuilder query = new StringBuilder("");
		Iterator<String> iterator = params.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			query.append(URLEncoder.encode(key));
			query.append("=");
			query.append(URLEncoder.encode(params.get(key)));
			if (iterator.hasNext())
				query.append("&");
		}
		
		return baseUrl + method + "." + format + "?" + query.toString();
	}
	
	public static Date parseDate(String date) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat(dateFormat);
		format.setTimeZone(DateUtils.GMT);
		return format.parse(date);
	}
	
	public static String fetchJSON(String url) throws CongressException {
		HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", userAgent + "-" + appVersion);
        
        if (extraHeaderKey != null && extraHeaderValue != null)
        	request.addHeader(extraHeaderKey, extraHeaderValue);
		
        DefaultHttpClient client = new DefaultHttpClient();
        
        try {
	        HttpResponse response = client.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();
	        
	        if (statusCode == HttpStatus.SC_OK) {
	        	String body = EntityUtils.toString(response.getEntity());
	        	return body;
	        } else if (statusCode == HttpStatus.SC_NOT_FOUND){
	        	throw new CongressException.NotFound("404 Not Found from " + url);
	        } else {
	        	throw new CongressException("Bad status code " + statusCode + " on fetching JSON from " + url);
	        }
        } catch (ClientProtocolException e) {
	    	throw new CongressException(e, "Problem fetching JSON from " + url);
	    } catch (IOException e) {
	    	throw new CongressException(e, "Problem fetching JSON from " + url);
	    }
	}
}