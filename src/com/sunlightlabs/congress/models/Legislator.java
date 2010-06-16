package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Legislator implements Comparable<Legislator>, Serializable {
	private static final long serialVersionUID = 1L;
	
	public String id, bioguide_id, govtrack_id;
	public String first_name, last_name, nickname, name_suffix;
	public String title, party, state, district;
	public String gender, congress_office, website, phone, twitter_id, youtube_url;

		
	public String getName() {
		return firstName() + " " + last_name;
	}
	
	public String firstName() {
		if (nickname != null && nickname.length() > 0)
			return nickname;
		else
			return first_name;
	}
	
	public String titledName() {
		String name = title + ". " + getName();
		if (name_suffix != null && !name_suffix.equals(""))
			name += ", " + name_suffix;
		return name;
	}
	
	public String getOfficialName() {
		return last_name + ", " + firstName();
	}

	public String fullTitle() {
		String title = this.title;
		if (title.equals("Del"))
			return "Delegate";
		else if (title.equals("Com"))
			return "Resident Commissioner";
		else if (title.equals("Sen"))
			return "Senator";
		else // "Rep"
			return "Representative";
	}
	
	public String getDomain() {
		String district = this.district;
		if (district.equals("Senior Seat") || district.equals("Junior Seat"))
			return district;
		else if (district.equals("0"))
			return "At-Large";
		else
			return "District " + district;
	}
	
	public String youtubeUsername() {
		if (this.youtube_url == null)
			return null;
		
		Pattern p = Pattern.compile("http://(?:www\\.)?youtube\\.com/(?:user/)?(.*?)/?$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(this.youtube_url);
		boolean found = m.find();
		if (found)
			return m.group(1);
		else
			return "";
	}
	
	public static String partyName(String party) {
		if (party.equals("D"))
			return "Democrat";
		if (party.equals("R"))
			return "Republican";
		if (party.equals("I"))
			return "Independent";
		else
			return "";
	}
	
	public String getPosition(String stateName) {
		String district = this.district;

		String position = "";

		if (district.equals("Senior Seat"))
			position = "Senior Senator from " + stateName;
		else if (district.equals("Junior Seat"))
			position = "Junior Senator from " + stateName;
		else if (district.equals("0")) {
			if (title.equals("Rep"))
				position = "Representative for " + stateName + " At-Large";
			else
				position = fullTitle() + " for " + stateName;
		} else
			position = "Representative for " + stateName + "-" + district;

		return "(" + party + ") " + position;
	}

	public static String bioguideUrl(String bioguide_id) {
		return "http://bioguide.congress.gov/scripts/biodisplay.pl?index=" + bioguide_id;
	}
	
	public static String openCongressUrl(String govtrack_id) {
		return "http://www.opencongress.org/person/show/" + govtrack_id;
	}
	
	public static String govTrackUrl(String govtrack_id) {
		return "http://www.govtrack.us/congress/person.xpd?id=" + govtrack_id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public String toString() {
		return titledName();
	}
	
	public int compareTo(Legislator another) {
		return this.last_name.compareTo(another.last_name);
	}
	
}