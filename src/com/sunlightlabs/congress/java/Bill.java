package com.sunlightlabs.congress.java;

import java.util.ArrayList;
import java.util.Date;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Bill {
	
	// basic info
	public String id, code, type, state, chamber;
	public int session, number;
	
	// extended info
	public String short_title, official_title;
	public Date introduced_at, last_action_at, last_vote_at, enacted_at;
	
	public String summary;
	
	public Bill(JSONObject json) throws JSONException, DateParseException {
		id = json.getString("govtrack_id");
		code = json.getString("code");
		type = json.getString("type");
		state = json.getString("state");
		chamber = json.getString("chamber");
		session = json.getInt("session");
		//number = json.getInt("number");
		
		if (json.has("short_title"))
			short_title = json.getString("short_title");
		if (json.has("official_title"))
			official_title = json.getString("official_title");
		if (json.has("introduced_at"))
			introduced_at = getDate(json, "introduced_at");
		if (json.has("last_action_at"))
			last_action_at = getDate(json, "last_action_at");
		if (json.has("last_vote_at"))
			last_vote_at = getDate(json, "last_vote_at");
		if (json.has("enacted_at"))
			enacted_at = getDate(json, "enacted_at");
		
		if (json.has("summary"))
			summary = json.getString("summary");
	}
	
	public static ArrayList<Bill> recentlyIntroduced(int n) throws CongressException {
		return billsFor(url("bills.json?sections=basic,extended&per_page=" + n));
	}
	
	
	public static ArrayList<Bill> billsFor(String url) throws CongressException {
		String rawJSON = Server.fetchJSON(url);
		ArrayList<Bill> bills = new ArrayList<Bill>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("bills");
			
			int length = results.length();
			for (int i = 0; i<length; i++)
				bills.add(new Bill(results.getJSONObject(i)));
				
		} catch(JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch(DateParseException e) {
			throw new CongressException(e, "Problem parsing a date from the JSON from " + url);
		}
		
		return bills;
	}
	
	private static String url(String path) {
		return Server.BASE_URL + "/" + path;
	}
	
	private Date getDate(JSONObject json, String key) throws JSONException, DateParseException {
		return DateUtils.parseDate(json.getString(key), Server.dateFormat);
	}
	
	public String toString() {
		if (short_title != null)
			return short_title;
		else if (official_title != null)
			return official_title;
		else
			return code;
	}
	
}