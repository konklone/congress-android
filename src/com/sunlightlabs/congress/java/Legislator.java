package com.sunlightlabs.congress.java;

import org.json.JSONException;
import org.json.JSONObject;

public class Legislator {
	
	public String bioguide_id, govtrack_id;
	public String first_name, last_name, nickname, name_suffix;
	public String title, party, state;
	
	public Legislator(JSONObject json) throws JSONException {
		bioguide_id = json.getString("bioguide_id");
		govtrack_id = json.getString("govtrack_id");
		
		if (json.has("first_name"))
			first_name = json.getString("first_name");
		if (json.has("last_name"))
			last_name = json.getString("last_name");
		if (json.has("nickname"))
			nickname = json.getString("nickname");
		if (json.has("name_suffix"))
			name_suffix = json.getString("name_suffix");
		if (json.has("title"))
			title = json.getString("title");
		if (json.has("party"))
			party = json.getString("party");
		if (json.has("state"))
			state = json.getString("state");
	}
	
	public String getName() {
		if (first_name == null || first_name.length() == 0)
			return first_name + " " + last_name;
		else
			return nickname +  " " + last_name;
	}
	
	public String titledName() {
		String name = title + ". " + getName();
		if (name_suffix != null && !name_suffix.equals(""))
			name += " " + name_suffix;
		return name;
	}
	
}