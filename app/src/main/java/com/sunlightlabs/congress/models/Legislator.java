package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Legislator implements Comparable<Legislator>, Serializable {
	private static final long serialVersionUID = 1L;
	
	public String bioguide_id, govtrack_id;
	public String first_name, middle_name, last_name;
	public String title, party, state, district, chamber;
	public String gender, office, website, phone;
	public String twitter_id, youtube_id, facebook_id; 
	public String term_start, term_end, leadership_role;
	public boolean in_office, at_large;

    // Only used when in a cosponsor context
    public Date cosponsored_on;
	
	// Set during committee membership parsing, with side/title/rank.
	public Committee.Membership membership;

    // Committees a legislator is a member of.
	public List<Committee> committees;

    // TODO: replace first_name uses with display name function
	public String getName() {
		return first_name + " " + last_name;
	}
    public String getNameByLastName() { return last_name + ", " + first_name; }

	public String titledName() {
        if (title == null) {
            // This will be wrong for Delegates and Resident Commissioners,
            // But I can live with that. In many contexts, we'll have the title.
			if (chamber == null)
			    return getName();
            else if (chamber.equals("house"))
                return "Rep. " + getName();
            else if (chamber.equals("senate"))
                return "Sen. " + getName();
            else
                return getName();
        } else
            return title + ". " + getName();
	}

	// return any trailing .
	public static String trimTitle(String title) {
        return title.replace(".", "");
    }

	public static String[] splitName(String displayName) {
		String[] pieces = displayName.split(" ");
		String first_name = pieces[0];
		String last_name = pieces[pieces.length-1];

		if (
			last_name.equals("Jr.") ||
			last_name.equals("II") ||
			last_name.equals("III")
		)
			last_name = pieces[pieces.length-2] + " " + pieces[pieces.length-1];


		return new String[]{first_name, last_name};
	}

	// Used to parse long titles from Pro Publica API
	public static String shortTitle(String longTitle) {
		if (longTitle.equals("Representative"))
			return "Rep";
		// Can be "Senator, 3rd Class"
		else if (longTitle.startsWith("Senator"))
			return "Sen";
		else if (longTitle.equals("Delegate"))
			return "Del";
		else if (longTitle.equals("Resident Commissioner"))
			return "Com";

		return "";
	}

	public String fullTitle() {
		String title = this.title;
		switch (title) {
			case "Del":
				return "Delegate";
			case "Com":
				return "Resident Commissioner";
			case "Sen":
				return "Senator";
			default:
// "Rep"
				return "Representative";
		}
	}

    public String getOffice() {
        if (this.chamber.equals("house"))
            return this.office + " House Office Building";
        else
            return this.office + " Senate Office Building";
    }

	public static String bioguideUrl(String bioguide_id) {
		return "http://bioguide.congress.gov/scripts/biodisplay.pl?index=" + bioguide_id;
	}

	// "anything" can be anything
	public static String govTrackUrl(String govtrack_id) {
        return "https://www.govtrack.us/congress/members/anything/" + govtrack_id;
	}
	
	public String toString() {
		return titledName();
	}
	
	public int compareTo(Legislator another) {
		return this.last_name.compareTo(another.last_name);
	}

	
	public String twitterUrl() {
		if (this.twitter_id == null || this.twitter_id.equals(""))
			return null;
		return "https://twitter.com/" + this.twitter_id;
	}
	
	public String youtubeUrl() {
		if (this.youtube_id == null || this.youtube_id.equals(""))
			return null;
		return "https://www.youtube.com/" + this.youtube_id;
	}
	
	public String facebookUrl() {
		if (this.facebook_id == null || this.facebook_id.equals(""))
			return null;
		return "https://www.facebook.com/" + this.facebook_id;
	}
}