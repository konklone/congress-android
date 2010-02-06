package com.sunlightlabs.congress.java;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Package of helper classes and server-wide info.
 */

public class Server {
	public static final String USER_AGENT = "Sunlight's Congress Android App (http://github.com/sunlightlabs/congress)";
	public static final String BASE_URL = "http://drumbone.sunlightlabs.com";
	
	public static final String[] dateFormat = new String[] {"yy/MM/dd HH:mm:ss Z"};
	
	public static String apiKey = "";
	public static String format = "json";
	
	public static String url(String method, String queryString) {
		if (queryString.length() > 0)
			queryString += "&";
		queryString += "apikey=" + apiKey;
		return Server.BASE_URL + "/" + method + "." + format + "?" + queryString;
	}
	
	public static String fetchJSON(String url) throws CongressException {
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
	        	throw new CongressException("Bad status code " + statusCode + "on fetching JSON");
	        }
        } catch (ClientProtocolException e) {
	    	throw new CongressException(e, "Problem fetching JSON from " + url);
	    } catch (IOException e) {
	    	throw new CongressException(e, "Problem fetching JSON from " + url);
	    }
	}
}
