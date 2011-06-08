package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bill implements Serializable {
	private static final long serialVersionUID = 1L;

	// basic
	public String id, code, bill_type, chamber;
	public int session, number;
	public boolean abbreviated;
	
	public String short_title, official_title;
	public Date last_action_at, last_passage_vote_at;
	public int cosponsors_count;
	
	public Date introduced_at, house_passage_result_at, senate_passage_result_at;
	public Date vetoed_at, house_override_result_at, senate_override_result_at;
	public Date senate_cloture_result_at;
	public Date awaiting_signature_since, enacted_at;
	
	public boolean vetoed, awaiting_signature, enacted;
	public String house_passage_result, senate_passage_result, house_override_result, senate_override_result;
	public String senate_cloture_result;
	
	// sponsor
	public Legislator sponsor;
	
	// cosponsors
	public List<Legislator> cosponsors;
	
	// summary
	public String summary;
	
	// votes
	public String last_vote_result;
	public String last_vote_chamber;
	public List<Bill.Vote> passage_votes;
	
	// actions
	public List<Bill.Action> actions;
	
	// search result metadata (if coming from a search)
	public SearchResult search;

	public static class Action implements Serializable {
		private static final long serialVersionUID = 1L;
		public String type, text;
		public Date acted_at;
	}
	
	public static class Vote implements Serializable {
		private static final long serialVersionUID = 1L;
		public String result, text, how, passage_type, chamber, roll_id;
		public Date voted_at;
	}
	
	public static String normalizeCode(String code) {
		return code.toLowerCase().replaceAll("[^\\w\\d]", "").replace("con", "c").replace("joint", "j").replace(" ", "").replace(".", "");
	}
	
	public static boolean isCode(String code) {
		return Pattern.compile("^(hr|hres|hjres|hcres|s|sres|sjres|scres)(\\d+)$").matcher(code).matches();
	}
	
	public static String currentSession() {
		int year = new Date().getYear();
		return "" + (((year + 1901) / 2) - 894);
	}
	
	public static String formatId(String id) {
		String code = id.replaceAll("-\\d+$", "");
		return formatCode(code);
	}
	
	public static String matchText(String field) {
		if (field.equals("versions"))
			return "text";
		else if (field.equals("short_title"))
			return "title";
		else if (field.equals("popular_title"))
			return "nickname";
		else if (field.equals("official_title"))
			return "official title";
		else if (field.equals("summary"))
			return "summary";
		else if (field.equals("keywords"))
			return "official keywords";
		else
			return "";
	}
	
	// from a highlight hash, return the field name with the highest priority
	public static String matchField(Map<String,ArrayList<String>> highlight) {
		String[] priorities = new String[] {"popular_title", "short_title", "official_title", "summary", "versions", "keywords"};
		String field = null;
		
		for (int i=0; i<priorities.length; i++) {
			if (highlight.containsKey(priorities[i])) {
				field = priorities[i];
				return field;
			}
		}
		
		return field;
	}
	
	private static String truncate(String text, int length) {
		if (text.length() > length)
			return text.substring(0, length - 3) + "...";
		else
			return text;
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
	
	// in accordance with the syntax described here:
	// http://thomas.loc.gov/home/handles/help.html
	public static String thomasType(String type) {
		if (type.equals("hcres"))
			return "hconres";
		else if (type.equals("scres"))
			return "sconres";
		else
			return type;
	}
	
	// in accordance with the syntax described here:
	// http://thomas.loc.gov/home/handles/help.html
	public static String thomasUrl(String type, int number, int session) {
		return "http://hdl.loc.gov/loc.uscongress/legislation." + session + thomasType(type) + number;
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