package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bill implements Serializable {
	private static final long serialVersionUID = 1L;

	// basic
	public String id, bill_type, chamber;
	public int session, number;
	
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
	
	// TODO: marked for death:
	public boolean abbreviated;
	
	// sponsor
	public Legislator sponsor;
	
	// cosponsors
	public List<Legislator> cosponsors;
	
	// summary
	public String summary;
	
	// votes
	public List<Bill.Vote> passage_votes;
	
	// actions
	public List<Bill.Action> actions;
	
	// search result metadata (if coming from a search)
	public SearchResult search;
	
	// latest upcoming bill data
	public List<UpcomingBill> upcoming;
	
	// full text URLs (to GPO)
	// valid keys: "html", "xml", "pdf"
	public Map<String,String> urls;
	
	public Map<String,String> homepages;

	public static class Action implements Serializable {
		private static final long serialVersionUID = 1L;
		public String type, text;
		public Date acted_at;
	}
	
	public static class Vote implements Serializable {
		private static final long serialVersionUID = 1L;
		//todo: rename passage_type->vote_type
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
	
	public static String formatCode(String bill_id) {
		Pattern pattern = Pattern.compile("^([a-z]+)(\\d+)-\\d+$");
		Matcher matcher = pattern.matcher(bill_id);
		if (!matcher.find())
			return bill_id;
		
		String bill_type = matcher.group(1);
		int number = Integer.valueOf(matcher.group(2));
		return formatCode(bill_type, number);
	}
	
	public static String formatCode(String bill_type, int number) {
		if (bill_type.equals("hr"))
			return "H.R. " + number;
		else if (bill_type.equals("hres"))
			return "H. Res. " + number;
		else if (bill_type.equals("hjres"))
			return "H.J. Res. " + number;
		else if (bill_type.equals("hcres"))
			return "H.Con. Res. " + number;
		else if (bill_type.equals("s"))
			return "S. " + number;
		else if (bill_type.equals("sres"))
			return "S. Res. " + number;
		else if (bill_type.equals("sjres"))
			return "S.J. Res. " + number;
		else if (bill_type.equals("scres"))
			return "S.Con. Res. " + number;
		else
			return bill_type + number;
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
	
	// prioritizes GPO "html" version (really text),
	// then GPO PDF, then finally the THOMAS landing page
	public String bestFullTextUrl() {
		if (this.urls != null && this.urls.containsKey("html"))
			return urls.get("html");
		else if (this.urls != null && this.urls.containsKey("pdf"))
			return urls.get("pdf");
		else
			return thomasUrl(bill_type, number, session);
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
	
	/**
	 * Regex for finding bills that end in "of 2009" or the like:
	 *   * \s+   = one or more spaces (or other whitespace)
	 *   * of     = "of"
	 *   * \s+   = one or more spaces (or other whitespace)
	 *   * \d{4} = 4 digits in a row (we'll need to update this to {5} in late 9999)
	 *   * \s*   = zero or more spaces (probably unnecessary)
	 *   * $      = end of line
	 */
	public static Pattern NEWS_SEARCH_REGEX = Pattern.compile("\\s+of\\s+\\d{4}\\s*$", Pattern.CASE_INSENSITIVE);
	
	// for news searching, don't use legislator.titledName() because we don't want to use the name_suffix
	public static String searchTermFor(Bill bill) {
    	if (bill.short_title != null && !bill.short_title.equals(""))
    		return "\"" + NEWS_SEARCH_REGEX.matcher(bill.short_title).replaceFirst("") + "\" OR \"" + Bill.formatCode(bill.bill_type, bill.number) + "\"";
    	else
    		return "\"" + Bill.formatCode(bill.bill_type, bill.number) + "\"";
    }
	
	
	// filters down any upcoming bill data, if any, to ones that happen either today or in the future 
	public List<UpcomingBill> upcomingSince(Date since) {
		List<UpcomingBill> results = new ArrayList<UpcomingBill>();
		
		if (upcoming == null || upcoming.size() == 0)
			return null;
		
		SimpleDateFormat testFormat = new SimpleDateFormat("yyyy-MM-dd");
		String testSince = testFormat.format(since);
		
		for (int i=0; i<upcoming.size(); i++) {
			UpcomingBill current = upcoming.get(i);
			
			// to make sure this comparison goes well and ignores time zones, we'll
			// convert both comparison dates to a YYYY-MM-DD timestamp, and then re-parse them
			// to be the same time zone at the same time of day, then compare.
			// Yes, I am aware that this is ridiculous, and betrays a willful ignorance of Java timezone utilities.
			String testCurrent = testFormat.format(current.legislativeDay);
			
			try {
				Date midnightSince = testFormat.parse(testSince);
				Date midnightCurrent = testFormat.parse(testCurrent);
				if (midnightSince.compareTo(midnightCurrent) <= 0)
					results.add(current);
			} catch (ParseException ex) {
				return null;
			}
		}
		
		return results;
	}
}