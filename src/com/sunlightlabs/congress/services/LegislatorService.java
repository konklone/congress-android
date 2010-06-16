package com.sunlightlabs.congress.services;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.net.Uri;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorService {
	
	/* Main methods */
	
	public static ArrayList<Legislator> allWhere(String key, String value) throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.getList", key + "=" + Uri.encode(value))); // encode user entered data
	}

	public static ArrayList<Legislator> allForZipCode(String zip) throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.allForZip", "zip=" + Uri.encode(zip))); // encode user entered data
	}

	public static ArrayList<Legislator> allForLatLong(double latitude, double longitude)
			throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.allForLatLong", "latitude=" + latitude
				+ "&longitude=" + longitude));
	}

	public static Legislator find(String bioguideId) throws CongressException {
		return legislatorFor(Sunlight.url("legislators.get", "bioguide_id=" + bioguideId));
	}
	
	public static Legislator fromCursor(Cursor c) {
		Legislator legislator = new Legislator();
		legislator.id = c.getString(c.getColumnIndex("id"));
		legislator.bioguide_id = c.getString(c.getColumnIndex("bioguide_id"));
		legislator.govtrack_id = c.getString(c.getColumnIndex("govtrack_id"));
		legislator.first_name = c.getString(c.getColumnIndex("first_name"));
		legislator.last_name = c.getString(c.getColumnIndex("last_name"));
		legislator.nickname = c.getString(c.getColumnIndex("nickname"));
		legislator.name_suffix = c.getString(c.getColumnIndex("name_suffix"));
		legislator.title = c.getString(c.getColumnIndex("title"));
		legislator.party = c.getString(c.getColumnIndex("party"));
		legislator.state = c.getString(c.getColumnIndex("state"));
		legislator.district = c.getString(c.getColumnIndex("district"));
		legislator.gender = c.getString(c.getColumnIndex("gender"));
		legislator.congress_office = c.getString(c.getColumnIndex("congress_office"));
		legislator.website = c.getString(c.getColumnIndex("website"));
		legislator.phone = c.getString(c.getColumnIndex("phone"));
		legislator.twitter_id = c.getString(c.getColumnIndex("twitter_id"));
		legislator.youtube_url = c.getString(c.getColumnIndex("youtube_url"));

		return legislator;
	}
	
	/* JSON parsers, also useful for other service endpoints within this package */

	protected static Legislator fromDrumbone(JSONObject json) throws JSONException {
		Legislator legislator = new Legislator();

		if (!json.isNull("bioguide_id"))
			legislator.bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			legislator.govtrack_id = json.getString("govtrack_id");
		legislator.setId(legislator.bioguide_id);

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

	protected static Legislator fromSunlight(JSONObject json) throws JSONException {
		Legislator legislator = new Legislator();

		if (!json.isNull("bioguide_id"))
			legislator.bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			legislator.govtrack_id = json.getString("govtrack_id");
		legislator.setId(legislator.bioguide_id);

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

	private static ArrayList<Legislator> legislatorsFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		ArrayList<Legislator> legislators = new ArrayList<Legislator>();
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