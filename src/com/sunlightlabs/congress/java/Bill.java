package com.sunlightlabs.congress.java;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Bill {
	
	// basic
	public String id, code, type, state, chamber;
	public int session, number;
	
	// extended
	public String short_title, official_title;
	public Date introduced_at, last_action_at, last_vote_at, enacted_at;
	
	// sponsor
	public Legislator sponsor;
	
	// summary
	public String summary;
	
	public Bill(JSONObject json) throws JSONException, DateParseException {
		id = json.getString("govtrack_id");
		code = json.getString("code");
		type = json.getString("type");
		state = json.getString("state");
		chamber = json.getString("chamber");
		session = json.getInt("session");
		number = json.getInt("number");
		
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
		
		if (json.has("sponsor"))
			sponsor = new Legislator(json.getJSONObject("sponsor"));
		
		if (json.has("summary"))
			summary = json.getString("summary");
	}
	
	public static ArrayList<Bill> recentlyIntroduced(int n) throws CongressException {
		return billsFor(url("bills.json?sections=basic,extended,sponsor&per_page=" + n));
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
	
	public String getTitle() {
		if (short_title != null)
			return short_title;
		else if (official_title != null)
			return official_title;
		else
			return formatCode(code);
	}
	
	public static String formatCode(String code) {
		Pattern pattern = Pattern.compile("^([a-z]+)(\\d+)$");
		Matcher matcher = pattern.matcher(code);
		if (!matcher.matches())
			return code;
		
		String match = matcher.group(1);
		String number = matcher.group(2);
		if (match.equals("hr"))
			return "H.R. " + number;
		else if (match.equals("hres"))
			return "H.Res. " + number;
		else if (match.equals("hjres"))
			return "H.J.Res. " + number;
		else if (match.equals("hcres"))
			return "H.C.Res. " + number;
		else if (match.equals("s"))
			return "S. " + number;
		else if (match.equals("sres"))
			return "S.Res. " + number;
		else if (match.equals("sjres"))
			return "S.J.Res. " + number;
		else if (match.equals("scres"))
			return "S.C.Res. " + number;
		else
			return code;
	}
}