package com.sunlightlabs.youtube;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.format.Time;

public class Video {
	public String title, description, url, thumbnailUrl;
	public Time timestamp;
	
	public Video(String xml) {
		this.title = parseValue("<title type=[\'\"]text[\'\"]>(.*?)</title>", xml);
		this.description = parseValue("<content type=[\'\"]text[\'\"]>(.*?)</content>", xml);
		
		// e.g. <link rel='alternate' type='text/html' href='http://www.youtube.com/watch?v=LnLnClV9ygY&amp;feature=youtube_gdata'/>
		this.url = parseValue("<link rel=[\'\"]alternate[\'\"] type=[\'\"]text/html[\'\"] href=[\'\"](.*?)[\'\"]\\s?/>", xml);
		this.url = this.url.replace("&amp;", "&");
		
		// e.g. <media:thumbnail url='http://i.ytimg.com/vi/LnLnClV9ygY/1.jpg' height='90'
		this.thumbnailUrl = parseValue("<media:thumbnail url=[\'\"](.*?)[\'\"] height", xml);
		
		String published = parseValue("<published>(.*?)</published>", xml);
		this.timestamp = new Time();
		this.timestamp.parse3339(published);
	}
	
	public String parseValue(String pattern, String body) {
		Pattern p = Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(body);
		if (m.find())
			return m.group(1);
		else
			return "";
	}
	
}
