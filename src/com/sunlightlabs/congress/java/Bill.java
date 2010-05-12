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
	public boolean passed;
	public String short_title, official_title;
	public Date introduced_at, last_action_at, last_vote_at, enacted_at;
	
	// sponsor
	public Legislator sponsor;
	
	// summary
	public String summary;
	
	// votes
	public String last_vote_result;
	public String last_vote_chamber;
	
	public Bill(JSONObject json) throws JSONException, DateParseException {
		if (!json.isNull("bill_id"))
			id = json.getString("bill_id");
		if (!json.isNull("code"))
			code = json.getString("code");
		if (!json.isNull("type"))
			type = json.getString("type");
		if (!json.isNull("state"))
			state = json.getString("state");
		if (!json.isNull("chamber"))
			chamber = json.getString("chamber");
		if (!json.isNull("session"))
			session = json.getInt("session");
		if (!json.isNull("number"))
			number = json.getInt("number");
		if (!json.isNull("passed"))
			passed = json.getBoolean("passed");
		
		if (!json.isNull("short_title"))
			short_title = json.getString("short_title");
		if (!json.isNull("official_title"))
			official_title = json.getString("official_title");
		if (!json.isNull("introduced_at"))
			introduced_at =  DateUtils.parseDate(json.getString("introduced_at"), Drumbone.dateFormat);
		if (!json.isNull("last_action_at"))
			last_action_at = DateUtils.parseDate(json.getString("last_action_at"), Drumbone.dateFormat);
		if (!json.isNull("last_vote_at"))
			last_vote_at = DateUtils.parseDate(json.getString("last_vote_at"), Drumbone.dateFormat);
		if (!json.isNull("enacted_at"))
			enacted_at = DateUtils.parseDate(json.getString("enacted_at"), Drumbone.dateFormat);
				
		if (!json.isNull("sponsor"))
			sponsor = Legislator.fromDrumbone(json.getJSONObject("sponsor"));
		
		if (!json.isNull("summary"))
			summary = json.getString("summary");
		
		if(!json.isNull("votes")) {
			JSONArray votes = json.getJSONArray("votes");
			int length = votes.length();
			
			for(int i = 0; i < length; i++) {
				JSONObject vote = votes.getJSONObject(i);
				
				// find the last vote (same date and year as the 'last_vote_at')
				if(DateUtils.parseDate(vote.getString("voted_at"), Drumbone.dateFormat).equals(last_vote_at)) { 					
					last_vote_chamber = vote.getString("chamber");
					last_vote_result = vote.getString("result");
					break;
				}
			}
		}
	}
		
	public static ArrayList<Bill> recentlyIntroduced(int n, int p)
			throws CongressException {
		return billsFor(Drumbone.url("bills",
				"order=introduced_at&sections=basic,sponsor&per_page=" + n + "&page=" + p));
	}
	
	public static ArrayList<Bill> recentLaws(int n, int p) throws CongressException {
		return billsFor(Drumbone
				.url("bills", "order=enacted_at&enacted=true&sections=basic,sponsor&per_page=" + n
						+ "&page=" + p));
	}
	
	public static ArrayList<Bill> recentlySponsored(int n, String sponsor_id, int p)
			throws CongressException {
		return billsFor(Drumbone.url("bills", "order=introduced_at&sponsor_id=" + sponsor_id
				+ "&sections=basic,sponsor&per_page=" + n + "&page=" + p));
	}
	
	public static ArrayList<Bill> latestVotes(int n, int p) throws CongressException {
		return billsFor(Drumbone.url("bills",
				"order=last_vote_at&sections=basic,sponsor,votes&per_page=" + n + "&page=" + p));
	}
	
	public static Bill find(String id, String sections) throws CongressException {
		return billFor(Drumbone.url("bill", "bill_id=" + id + "&sections=" + sections));
	}
	
	public static Bill billFor(String url) throws CongressException {
		String rawJSON = Drumbone.fetchJSON(url);
		try {
			return new Bill(new JSONObject(rawJSON).getJSONObject("bill"));
		} catch(JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch(DateParseException e) {
			throw new CongressException(e, "Problem parsing a date from the JSON from " + url);
		}
	}
	
	public static ArrayList<Bill> billsFor(String url) throws CongressException {
		String rawJSON = Drumbone.fetchJSON(url);
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
			return "H. Res. " + number;
		else if (match.equals("hjres"))
			return "H. Joint Res. " + number;
		else if (match.equals("hcres"))
			return "H. Con. Res. " + number;
		else if (match.equals("s"))
			return "S. " + number;
		else if (match.equals("sres"))
			return "S. Res. " + number;
		else if (match.equals("sjres"))
			return "S. Joint Res. " + number;
		else if (match.equals("scres"))
			return "S. Con. Res. " + number;
		else
			return code;
	}
	
	public static String thomasType(String type) {
		if (type.equals("hr"))
			return "h";
		else if (type.equals("hres"))
			return "hr";
		else if (type.equals("hjres"))
			return "hj";
		else if (type.equals("hcres"))
			return "hc";
		else if (type.equals("s"))
			return "s";
		else if (type.equals("sres"))
			return "sr";
		else if (type.equals("sjres"))
			return "sj";
		else if (type.equals("scres"))
			return "sc";
		else
			return type;
	}
	
	public static String thomasUrl(Bill bill) {
		return "http://thomas.loc.gov/cgi-bin/query/z?c" + bill.session + ":" + thomasType(bill.type) + bill.number + ":";
	}
	
	public static String formatSummary(String summary, String short_title) {
		String formatted = summary;
		formatted = formatted.replaceFirst("^\\d+\\/\\d+\\/\\d+--.+?\\.\\s*", "");
		formatted = formatted.replaceFirst("(\\(This measure.+?\\))\n*\\s*", "");
		if (short_title != null)
			formatted = formatted.replaceFirst("^" + short_title + " - ", "");
		formatted = formatted.replaceAll("\n", "\n\n");
		formatted = formatted.replaceAll(" (\\(\\d\\))", "\n\n$1");
		formatted = formatted.replaceAll("( [^A-Z\\s]+\\.)\\s+", "$1\n\n");
		return formatted;
	}
	
	public static String displayTitle(Bill bill) {
		return (bill.short_title != null) ? bill.short_title : bill.official_title; 
	}

	
}