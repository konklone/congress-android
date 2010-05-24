package com.sunlightlabs.congress.java;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.net.Uri;

/**
 * Package of helper classes and server-wide info.
 */

public class Sunlight {
	public static final String BASE_URL = "http://services.sunlightlabs.com/api/";
	public static final String USER_AGENT = "sunlight-congress";
	
	public static String apiKey = "";
	public static String appVersion = "";
	public static String format = "json";
	
	
	public static String url(String method, String queryString) {
		if (queryString.length() > 0)
			queryString += "&";
		queryString += "apikey=" + apiKey;
		return BASE_URL + method + "." + format + "?" + queryString;
	}
	
	public static String fetchJSON(String url) throws CongressException {
		HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", USER_AGENT + "-" + appVersion);
		
        DefaultHttpClient client = new DefaultHttpClient();
        
        try {
	        HttpResponse response = client.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();
	        
	        if (statusCode == HttpStatus.SC_OK) {
	        	String body = EntityUtils.toString(response.getEntity());
	        	return body;
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