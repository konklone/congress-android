package com.sunlightlabs.congress.services;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.sunlightlabs.congress.models.CongressException;


public class Drumbone {
	public static final String[] dateFormat = new String[] {"yy/MM/dd HH:mm:ss Z"};
	
	public static final String baseUrl = "http://drumbone.services.sunlightlabs.com/v1/api/";
	public static final String format = "json";
	
	// filled in by the client
	public static String userAgent = "com.sunlightlabs.congress.services.Drumbone";
	public static String appVersion = "unversioned";
	public static String apiKey = "";
	
	// optional, if you want to add a custom header/value for your environment
	public static String extraHeaderKey = null;
	public static String extraHeaderValue = null;
	
	
	public static String url(String method, String queryString) {
		if (queryString.length() > 0)
			queryString += "&";
		queryString += "apikey=" + apiKey;
		return baseUrl + method + "." + format + "?" + queryString;
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