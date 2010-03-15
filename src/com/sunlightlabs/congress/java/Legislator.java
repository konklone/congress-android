package com.sunlightlabs.congress.java;

import org.json.JSONException;
import org.json.JSONObject;

public class Legislator {
	
	public String bioguide_id, govtrack_id;
	public String first_name, last_name, nickname, name_suffix;
	public String title, party, state, district;
	
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