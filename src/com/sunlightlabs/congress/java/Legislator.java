package com.sunlightlabs.congress.java;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Legislator implements Comparable<Legislator> {
	
	public String bioguide_id, govtrack_id;
	public String first_name, last_name, nickname, name_suffix;
	public String title, party, state, district;
	public String gender, congress_office, website, phone, twitter_id, youtube_url;
	
	private Legislator() {}
	
	
	// all legislators meeting one condition
	public static ArrayList<Legislator> allWhere(String key, String value) throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.getList", key + "=" + value));
	}
	
	public static ArrayList<Legislator> allForZipCode(String zip) throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.allForZip", "zip=" + zip));
	}
	
	public static ArrayList<Legislator> allForLatLong(double latitude, double longitude) throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.allForLatLong", "latitude=" + latitude + "&longitude=" + longitude));
	}
	
	public static Legislator find(String bioguide_id) throws CongressException {
		return legislatorFor(Sunlight.url("legislators.get", "bioguide_id=" + bioguide_id));
	}
	
	private static Legislator legislatorFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		try {
			return Legislator.fromSunlight(new JSONObject(rawJSON).getJSONObject("response").getJSONObject("legislator"));
		} catch(JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}
	
	private static ArrayList<Legislator> legislatorsFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		ArrayList<Legislator> legislators = new ArrayList<Legislator>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONObject("response").getJSONArray("legislators");
			
			int length = results.length();
			for (int i = 0; i<length; i++)
				legislators.add(Legislator.fromSunlight(results.getJSONObject(i).getJSONObject("legislator")));
				
		} catch(JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
		
		return legislators;
	}
	
	public static Legislator fromDrumbone(JSONObject json) throws JSONException {
		Legislator legislator = new Legislator();
		
		if (!json.isNull("bioguide_id"))
			legislator.bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			legislator.govtrack_id = json.getString("govtrack_id");
		
		if (!json.isNull("first_name"))
			legislator.first_name = json.getString("first_name");
		if (!json.isNull("last_name"))
			legislator.last_name = json.getString("last_name");
		if (!json.isNull("nickname"))
			legislator.nickname = json.getString("nickname");
		if (!json.isNull("name_suffix"))
			legislator.name_suffix = json.getString("name_suffix");
		if (!json.isNull("title"))
			legislator.title = json.getString("title");
		if (!json.isNull("party"))
			legislator.party = json.getString("party");
		if (!json.isNull("state"))
			legislator.state = json.getString("state");
		if (!json.isNull("district"))
			legislator.district = json.getString("district");
		
		if (!json.isNull("gender"))
			legislator.gender = json.getString("gender");
		if (!json.isNull("congress_office"))
			legislator.congress_office = json.getString("congress_office");
		if (!json.isNull("website"))
			legislator.website = json.getString("website");
		if (!json.isNull("phone"))
			legislator.phone = json.getString("phone");
		if (!json.isNull("youtube_url"))
			legislator.youtube_url = json.getString("youtube_url");
		if (!json.isNull("twitter_id"))
			legislator.twitter_id = json.getString("twitter_id");
		
		return legislator;
	}
	
	public static Legislator fromSunlight(JSONObject json) throws JSONException {
		Legislator legislator = new Legislator();
		
		if (!json.isNull("bioguide_id"))
			legislator.bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			legislator.govtrack_id = json.getString("govtrack_id");
		
		if (!json.isNull("firstname"))
			legislator.first_name = json.getString("firstname");
		if (!json.isNull("lastname"))
			legislator.last_name = json.getString("lastname");
		if (!json.isNull("nickname"))
			legislator.nickname = json.getString("nickname");
		if (!json.isNull("name_suffix"))
			legislator.name_suffix = json.getString("name_suffix");
		if (!json.isNull("title"))
			legislator.title = json.getString("title");
		if (!json.isNull("party"))
			legislator.party = json.getString("party");
		if (!json.isNull("state"))
			legislator.state = json.getString("state");
		if (!json.isNull("district"))
			legislator.district = json.getString("district");
		
		if (!json.isNull("gender"))
			legislator.gender = json.getString("gender");
		if (!json.isNull("congress_office"))
			legislator.congress_office = json.getString("congress_office");
		if (!json.isNull("website"))
			legislator.website = json.getString("website");
		if (!json.isNull("phone"))
			legislator.phone = json.getString("phone");
		if (!json.isNull("youtube_url"))
			legislator.youtube_url = json.getString("youtube_url");
		if (!json.isNull("twitter_id"))
			legislator.twitter_id = json.getString("twitter_id");
		
		return legislator;
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
	
	public String getId() {
		return bioguide_id;
	}
	
	public String toString() {
		return titledName();
	}
	
	public int compareTo(Legislator another) {
		return this.last_name.compareTo(another.last_name);
	}
	
}