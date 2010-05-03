package com.sunlightlabs.congress.java;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

public class Legislator {
	
	public String bioguide_id, govtrack_id;
	public String first_name, last_name, nickname, name_suffix;
	public String title, party, state, district;
	public String gender, congress_office, website, phone, twitter_id, youtube_url;
	
	public Legislator(JSONObject json) throws JSONException {
		if (!json.isNull("bioguide_id"))
			bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			govtrack_id = json.getString("govtrack_id");
		
		if (!json.isNull("first_name"))
			first_name = json.getString("first_name");
		if (!json.isNull("last_name"))
			last_name = json.getString("last_name");
		if (!json.isNull("nickname"))
			nickname = json.getString("nickname");
		if (!json.isNull("name_suffix"))
			name_suffix = json.getString("name_suffix");
		if (!json.isNull("title"))
			title = json.getString("title");
		if (!json.isNull("party"))
			party = json.getString("party");
		if (!json.isNull("state"))
			state = json.getString("state");
		if (!json.isNull("district"))
			district = json.getString("district");
		
		if (!json.isNull("gender"))
			gender = json.getString("gender");
		if (!json.isNull("congress_office"))
			gender = json.getString("congress_office");
		if (!json.isNull("website"))
			gender = json.getString("website");
		if (!json.isNull("phone"))
			gender = json.getString("phone");
		if (!json.isNull("youtube_url"))
			gender = json.getString("youtube_url");
		if (!json.isNull("twitter_id"))
			gender = json.getString("twitter_id");
	}
	
	public String getName() {
		return firstName() + " " + last_name;
	}
	
	public String firstName() {
		if (first_name == null || first_name.length() == 0)
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
		String url = this.youtube_url;
		Pattern p = Pattern.compile("http://(?:www\\.)?youtube\\.com/(?:user/)?(.*?)/?$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(url);
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
	
}