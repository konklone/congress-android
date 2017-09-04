package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bill implements Serializable {
	private static final long serialVersionUID = 1L;

	// basic
	public String id, bill_type, chamber;
	public int congress, number;

    // TODO: chop down to just title
	public String short_title, official_title;
    public Date last_action_on;

    public Date introduced_on, house_passage_result_on, senate_passage_result_on;
	public Date vetoed_on, enacted_on;
    public boolean vetoed, enacted;

    // whether a bill made it past routine introduction/referral
    public boolean active;
    public int cosponsors_count;

    // summary
    public String summary;

    // official URLs
    public String govtrack_url, congress_url, gpo_url;

	// sponsor
	public Legislator sponsor;

	// cosponsors
	public List<Legislator> cosponsors;

	// votes
	public List<Bill.Vote> votes;
	
	// actions
	public List<Bill.Action> actions;
	public Bill.Action lastAction;


	public static class Action implements Serializable {
		private static final long serialVersionUID = 1L;

		public int id;
		public String full_id; // bill_id + "-" + id
        public String type, description, chamber;
		public Date acted_on;
	}
	
	public static class Vote implements Serializable {
		private static final long serialVersionUID = 1L;

        // bill_id + "-" + roll_id
        // This works as long as these are all roll call votes.
        public String full_id;

        public String roll_id, result, question, chamber;
        public int yes, no, not_voting;

        // TODO: move this back to voted_at
		public Date voted_on;

		public boolean passed() {
			if (this.result == null) return false;

            Pattern pattern = Pattern.compile("passed", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(this.result);
            boolean passed = matcher.find();
            return passed;
		}
	}
	
	public static String normalizeCode(String code) {
		return code.toLowerCase(Locale.US).replaceAll("[^\\w\\d]", "").replace("con", "c").replace("joint", "j").replace(" ", "").replace(".", "");
	}

	public static String chamberFrom(String bill_type) {
        if (bill_type.startsWith("h"))
            return "house";
        else if (bill_type.startsWith("s"))
            return "senate";
        else
            return null;
    }
	
	public static boolean isCode(String code) {
		return Pattern.compile("^(hr|hres|hjres|hconres|s|sres|sjres|sconres)(\\d+)$").matcher(code).matches();
	}
	
	public static int currentCongress() {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		return congressForYear(year);
	}

	public static int congressForYear(int year) {
        return ((year + 1) / 2) - 894;
    }

    public static int sessionForYear(int year) {
        // odd years are the first session of a 2-year congress
        if (year % 2 == 1)
            return 1;
        // even years are the second session of a 2-year congress
        else
            return 2;
    }

    public static int yearFrom(int congress, int session) {
        // first year of that Congress
        int base = (((congress + 894) * 2) - 1);

        // if it's session 2, up it by 1 year
        return base + (session - 1);
    }

	public static String formatCode(String bill_id) {
		// [bill_type, number, congress]
        String[] pieces = splitBillId(bill_id);
        if (pieces == null) return bill_id;
		return formatCode(pieces[0], Integer.parseInt(pieces[1]));
	}

	// returns [bill_type, number, congress]
	public static String[] splitBillId(String bill_id) {
        Pattern pattern = Pattern.compile("^([a-z]+)(\\d+)-(\\d+)$");
        Matcher matcher = pattern.matcher(bill_id);
        if (!matcher.find())
            return null;

        String[] pieces = { matcher.group(1), matcher.group(2), matcher.group(3) };
        return pieces;
    }

    // also expanded to handle amendment IDs
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

        else if (bill_type.equals("hamdt"))
            return "H.Amdt. " + number;
        else if (bill_type.equals("samdt"))
            return "S.Amdt. " + number;

		else
			return bill_type + number;
	}
	
	// prioritizes GPO "html" version (really text),
	// then GPO PDF, then finally the THOMAS landing page
	public String bestFullTextUrl() {
		if (this.congress_url != null)
			return this.congress_url + "/text";
		else if (this.gpo_url != null)
			return this.gpo_url;
        else
            return this.govtrackTextUrl();
	}

	// next best thing to official, easily calculable from bill fields
	public String govtrackTextUrl() {
		return "https://www.govtrack.us/congress/bills/" + congress + "/" + bill_type + number + "/text";
	}

	// TODO: this may not be necessary anymore
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

}