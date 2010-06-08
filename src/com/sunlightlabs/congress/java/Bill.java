package com.sunlightlabs.congress.java;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class Bill {

	// basic
	public String id, code, type, state, chamber;
	public int session, number;
	public String short_title, official_title;
	public Date last_action_at, last_vote_at;
	
	public Date introduced_at, house_result_at, senate_result_at, passed_at;
	public Date vetoed_at, override_house_result_at, override_senate_result_at;
	public Date awaiting_signature_since, enacted_at;
	
	public boolean passed, vetoed, awaiting_signature, enacted;
	public String house_result, senate_result, override_house_result, override_senate_result;
	
	// sponsor
	public Legislator sponsor;
	
	// summary
	public String summary;
	
	// votes
	public String last_vote_result;
	public String last_vote_chamber;
	public ArrayList<Bill.Vote> votes = new ArrayList<Bill.Vote>();
	
	// actions
	public ArrayList<Bill.Action> actions = new ArrayList<Bill.Action>();
	

	public static class Action {
		public String type, text;
		public Date acted_at;
		
		public Action(JSONObject json) throws JSONException, DateParseException {
			text = json.getString("text");
			type = json.getString("type");
			acted_at = DateUtils.parseDate(json.getString("acted_at"), Drumbone.dateFormat);
		}
	}
	
	public static class Vote {
		public String result, text, how, type, chamber, roll_id;
		public Date voted_at;
		
		public Vote(JSONObject json) throws JSONException, DateParseException {
			result = json.getString("result");
			text = json.getString("text");
			how = json.getString("how");
			type = json.getString("type");
			chamber = json.getString("chamber");
			voted_at = DateUtils.parseDate(json.getString("voted_at"), Drumbone.dateFormat);
			
			if (!json.isNull("roll_id"))
				roll_id = json.getString("roll_id");
		}
	}
	
	// takes a potentially user entered, variably formatted code and transforms it into a bill_id
	public static String codeToBillId(String code) {
		return code.toLowerCase().replace(" ", "").replace(".", "") + "-" + currentSession();
	}
	
	public static String currentSession() {
		int year = new Date().getYear();
		return "" + (((year + 1901) / 2) - 894);
	}
	
	public static String formatCode(String code) {
		code = code.toLowerCase().replace(" ", "").replace(".", "");
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
	
	// for when you need that extra space
	public static String formatCodeShort(String code) {
		code = code.toLowerCase().replace(" ", "").replace(".", "");
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
			return "H.J. Res. " + number;
		else if (match.equals("hcres"))
			return "H.C. Res. " + number;
		else if (match.equals("s"))
			return "S. " + number;
		else if (match.equals("sres"))
			return "S. Res. " + number;
		else if (match.equals("sjres"))
			return "S.J. Res. " + number;
		else if (match.equals("scres"))
			return "S.C. Res. " + number;
		else
			return code;
	}
	
	public static String govTrackType(String type) {
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
	
	public static String thomasUrl(String type, int number, int session) {
		return "http://thomas.loc.gov/cgi-bin/query/z?c" + session + ":" + type + number + ":";
	}
	
	public static String openCongressUrl(String type, int number, int session) {
		return "http://www.opencongress.org/bill/" + session + "-" + govTrackType(type) + number + "/show";
	}
	
	public static String govTrackUrl(String type, int number, int session) {
		return "http://www.govtrack.us/congress/bill.xpd?bill=" + govTrackType(type) + session + "-" + number;
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