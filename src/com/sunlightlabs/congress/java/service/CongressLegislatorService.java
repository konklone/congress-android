package com.sunlightlabs.congress.java.service;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Legislator;
import com.sunlightlabs.congress.java.Sunlight;

public class CongressLegislatorService implements LegislatorService {

	public Legislator fromDrumbone(JSONObject json) throws CongressException {
		Legislator legislator = new Legislator();

		try {
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
		} catch (JSONException e) {
			throw new CongressException(e, "Could not instantiate a new Legislator from Drumbone.");
		}

		return legislator;
	}

	public Legislator fromSunlight(JSONObject json) throws CongressException {
		Legislator legislator = new Legislator();

		try {
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
		} catch (JSONException e) {
			throw new CongressException(e, "Could not instantiate a new Legislator from Sunlight.");
		}

		return legislator;
	}

	public ArrayList<Legislator> allWhere(String key, String value) throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.getList", key + "=" + Uri.encode(value))); // encode user entered data
	}

	public ArrayList<Legislator> allForZipCode(String zip) throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.allForZip", "zip=" + Uri.encode(zip))); // encode user entered data
	}

	public ArrayList<Legislator> allForLatLong(double latitude, double longitude)
			throws CongressException {
		return legislatorsFor(Sunlight.url("legislators.allForLatLong", "latitude=" + latitude
				+ "&longitude=" + longitude));
	}

	public Legislator find(String bioguideId) throws CongressException {
		return legislatorFor(Sunlight.url("legislators.get", "bioguide_id=" + bioguideId));
	}

	public Legislator legislatorFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		try {
			return fromSunlight(new JSONObject(rawJSON).getJSONObject("response")
					.getJSONObject("legislator"));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

	public ArrayList<Legislator> legislatorsFor(String url) throws CongressException {
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
