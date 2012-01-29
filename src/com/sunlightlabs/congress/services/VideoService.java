package com.sunlightlabs.congress.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Video;

public class VideoService {
	
	public static Video latest(String chamber) throws CongressException {
		String[] sections = new String[] {"basic"};
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("chamber", chamber);
		
		return videoFor(RealTimeCongress.url("videos", sections, params, 1, 1));
	}
	
	public static Video forDay(String chamber, String legislativeDay) throws CongressException {
		String[] sections = new String[] {"basic"};
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("chamber", chamber);
		params.put("legislative_day", legislativeDay);
		
		return videoFor(RealTimeCongress.url("videos", sections, params, 1, 1));
	}
	
	protected static Video fromRTC(JSONObject json) throws JSONException, ParseException {
		Video video = new Video();
		
		if (!json.isNull("chamber"))
			video.chamber = json.getString("chamber");
		
		if (!json.isNull("session"))
			video.session = json.getInt("session");
		
		if (!json.isNull("duration"))
			video.duration = json.getInt("duration");
		
		if (!json.isNull("video_id"))
			video.videoId = json.getString("video_id");
		
		if (!json.isNull("clip_id"))
			video.clipId = json.getString("clip_id");
		
		if (!json.isNull("legislative_day"))
			video.legislativeDay = RealTimeCongress.parseDateOnly(json.getString("legislative_day"));
		
		if (!json.isNull("pubdate"))
			video.pubdate = parseDate(json.getString("pubdate"));
		
		if (!json.isNull("bioguide_ids"))
			video.bioguideIds = RealTimeCongress.listFrom(json.getJSONArray("bioguide_ids"));
		
		if (!json.isNull("bill_ids"))
			video.billIds = RealTimeCongress.listFrom(json.getJSONArray("bill_ids"));
		
		if (!json.isNull("roll_ids"))
			video.rollIds = RealTimeCongress.listFrom(json.getJSONArray("roll_ids"));
		
		if (!json.isNull("clip_urls")) {
			video.clipUrls = new HashMap<String,String>();
			JSONObject urls = json.getJSONObject("clip_urls");
			if (!urls.isNull("hls"))
				video.clipUrls.put("hls", urls.getString("hls"));
			if (!urls.isNull("mp4"))
				video.clipUrls.put("mp4", urls.getString("mp4"));
			if (!urls.isNull("rtmp"))
				video.clipUrls.put("rtmp", urls.getString("rtmp"));
		}
		
		return video;
	}
	
	private static Date parseDate(String date) throws ParseException {
		String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
		SimpleDateFormat format = new SimpleDateFormat(dateFormat);
		format.setTimeZone(DateUtils.GMT);
		return format.parse(date);
	}
	
	private static Video videoFor(String url) throws CongressException {
		String rawJSON = RealTimeCongress.fetchJSON(url);
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("videos");
			if (results.length() == 0)
				throw new CongressException.NotFound("Video not found.");
			else
				return fromRTC(results.getJSONObject(0));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
	}
}