package com.sunlightlabs.android.youtube;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class YouTube {
		
	public Video[] getVideos(String username) throws YouTubeException {
		ArrayList<Video> videos = new ArrayList<Video>();
		
		String xml = fetchXml(username);
		
		Pattern entryPattern = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL);
		Matcher entryMatcher = entryPattern.matcher(xml);
		while (entryMatcher.find()) {
			videos.add(new Video(entryMatcher.group(1)));
		}
		
		return videos.toArray(new Video[0]);
	}
	
	private String fetchXml(String username) throws YouTubeException {
		String url = feedUrl(username);
		HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", "Sunlight's Congress Android App (http://github.com/sunlightlabs/congress");
		
        DefaultHttpClient client = new DefaultHttpClient();
        
        try {
	        HttpResponse response = client.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();
	        
	        if (statusCode == HttpStatus.SC_OK) {
	        	String body = EntityUtils.toString(response.getEntity());
	        	return body;
	        } else {
	        	throw new YouTubeException("Bad status code on fetching news");
	        }
        } catch (Exception e) {
	    	throw new YouTubeException(e);
	    }
	}
	
	private String feedUrl(String username) {
		return "http://gdata.youtube.com/feeds/api/users/" + username + "/uploads?orderby=updated"; 
	}
}