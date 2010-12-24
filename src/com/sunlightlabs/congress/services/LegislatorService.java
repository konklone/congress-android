package com.sunlightlabs.congress.services;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorService {
	
	/* Main methods */
	
	public static List<Legislator> allWhere(String key, String value) throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.getList", key + "=" + Uri.encode(value))); // encode user entered data
	}

	public static List<Legislator> allForZipCode(String zip) throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.allForZip", "zip=" + Uri.encode(zip))); // encode user entered data
	}

	public static List<Legislator> allForLatLong(double latitude, double longitude)
			throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.allForLatLong", "latitude=" + latitude
				+ "&longitude=" + longitude));
	}

	public static Legislator find(String bioguideId) throws CongressException {
		return legislatorFor(Sunlight.url("legislators.get", "bioguide_id=" + bioguideId + "&all_legislators=true"));
	}
	
	/* JSON parsers, also useful for other service endpoints within this package */

	protected static Legislator fromDrumbone(JSONObject json) throws JSONException {
		Legislator legislator = new Legislator();

		if (!json.isNull("bioguide_id"))
			legislator.bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			legislator.govtrack_id = json.getString("govtrack_id");
		
		legislator.setId(legislator.bioguide_id);
		
		if (!json.isNull("in_office"))
			legislator.in_office = json.getBoolean("in_office");

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
		if (!json.isNull("chamber"))
			legislator.chamber = json.getString("chamber");

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

	protected static Legislator fromSunlight(JSONObject json) throws JSONException {
		Legislator legislator = new Legislator();

		if (!json.isNull("bioguide_id"))
			legislator.bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			legislator.govtrack_id = json.getString("govtrack_id");
		
		legislator.setId(legislator.bioguide_id);

		if (!json.isNull("in_office"))
			legislator.in_office = json.getBoolean("in_office");
		
		if (!json.isNull("firstname"))
			legislator.first_name = json.getString("firstname");
		if (!json.isNull("lastname"))
			legislator.last_name = json.getString("lastname");
		if (!json.isNull("nickname"))
			legislator.nickname = json.getString("nickname");
		if (!json.isNull("name_suffix"))
			legislator.name_suffix = json.getString("name_suffix");
		if (!json.isNull("party"))
			legislator.party = json.getString("party");
		if (!json.isNull("state"))
			legislator.state = json.getString("state");
		if (!json.isNull("district"))
			legislator.district = json.getString("district");
		if (!json.isNull("title"))
			legislator.title = json.getString("title");
		
		legislator.chamber = legislator.title.equals("Sen") ? "senate" : "house";

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
	
	
	/* Private helpers for loading single or plural bill objects */

	private static Legislator legislatorFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		try {
			return fromSunlight(new JSONObject(rawJSON).getJSONObject("response")
					.getJSONObject("legislator"));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

	private static List<Legislator> legislatorsFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		List<Legislator> legislators = new ArrayList<Legislator>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONObject("response").getJSONArray(
					"legislators");

			int length = results.length();
			for (int i = 0; i < length; i++)
				legislators.add(fromSunlight(results.getJSONObject(i).getJSONObject(
						"legislator")));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}

		return legislators;
	}

}