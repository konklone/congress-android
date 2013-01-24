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
	public String id, bill_type, chamber;
	public int congress, number;
	
	public String short_title, official_title;
	public Date last_action_at, last_passage_vote_at;
	public int cosponsors_count;
	
	public Date introduced_on, house_passage_result_at, senate_passage_result_at;
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
	public List<Bill.Vote> votes;
	
	// actions
	public List<Bill.Action> actions;
	
	// search result metadata (if coming from a search)
	public SearchResult search;
	
	// latest upcoming bill data
	public List<UpcomingBill> upcoming;
	
	// homepage URLs on services
	public Map<String,String> urls;
	
	// full text URLs (to GPO)
	// valid keys: "html", "xml", "pdf"
	public Map<String,String> versionUrls;

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
		return Pattern.compile("^(hr|hres|hjres|hconres|s|sres|sjres|sconres)(\\d+)$").matcher(code).matches();
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
		else if (bill_type.equals("hconres"))
			return "H.Con. Res. " + number;
		else if (bill_type.equals("s"))
			return "S. " + number;
		else if (bill_type.equals("sres"))
			return "S. Res. " + number;
		else if (bill_type.equals("sjres"))
			return "S.J. Res. " + number;
		else if (bill_type.equals("sconres"))
			return "S.Con. Res. " + number;
		else
			return bill_type + number;
	}
	
	// prioritizes GPO "html" version (really text),
	// then GPO PDF, then finally the THOMAS landing page
	public String bestFullTextUrl() {
		if (this.versionUrls != null && this.versionUrls.containsKey("html"))
			return versionUrls.get("html");
		else if (this.versionUrls != null && this.versionUrls.containsKey("pdf"))
			return versionUrls.get("pdf");
		else if (this.urls != null && this.urls.containsKey("congress"))
			return this.urls.get("congress");
		else
			return fallbackTextUrl();
	}
	
	// next best thing to official, easily calculable from bill fields
	public String fallbackTextUrl() {
		return "http://www.govtrack.us/congress/bills/" + congress + "/" + bill_type + number + "/text";
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
}