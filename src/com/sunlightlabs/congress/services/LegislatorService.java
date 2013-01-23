package com.sunlightlabs.congress.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorService {
	
	private static String[] basicFields = new String[] {
		"bioguide_id", "thomas_id", "govtrack_id",
		"in_office", "party", "gender", "state", "state_name",
		"district", "title", "chamber", "senate_class", "birthday",
		"term_start", "term_end",
		"first_name", "nickname", "middle_name", "last_name", "name_suffix",
		"phone", "website", "office",
		"twitter_id", "youtube_id", "facebook_id"
	};
	
	/* Main methods */
	
	public static List<Legislator> allWhere(String key, String value) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put(key, value);
		params.put("order", "last_name__asc");
		return legislatorsFor(Congress.url("legislators", basicFields, params));
	}

	public static List<Legislator> allForZipCode(String zip) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("zip", zip);
		return legislatorsFor(Congress.url("legislators/locate", basicFields, params));
	}

	public static List<Legislator> allForLatLong(double latitude, double longitude) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("latitude", String.valueOf(latitude));
		params.put("longitude", String.valueOf(longitude));
		return legislatorsFor(Congress.url("legislators/locate", basicFields, params));
	}
	
	public static Legislator find(String bioguideId) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("bioguide_id", bioguideId);
//		params.put("all_legislators", "true");
		return legislatorFor(Congress.url("legislators", basicFields, params));
	}
	
	/* JSON parsers, also useful for other service endpoints within this package */

	protected static Legislator fromAPI(JSONObject json) throws JSONException {
		Legislator legislator = new Legislator();

		if (!json.isNull("bioguide_id"))
			legislator.bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			legislator.govtrack_id = json.getString("govtrack_id");
		if (!json.isNull("thomas_id"))
			legislator.thomas_id = json.getString("thomas_id");
		
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
		if (!json.isNull("term_start"))
			legislator.term_start = json.getString("term_start");
		if (!json.isNull("term_end"))
			legislator.term_end = json.getString("term_end");

		if (!json.isNull("gender"))
			legislator.gender = json.getString("gender");
		if (!json.isNull("office"))
			legislator.office = json.getString("office");
		if (!json.isNull("website"))
			legislator.website = json.getString("website");
		if (!json.isNull("phone"))
			legislator.phone = json.getString("phone");
		if (!json.isNull("youtube_id"))
			legislator.youtube_id = json.getString("youtube_id");
		if (!json.isNull("twitter_id"))
			legislator.twitter_id = json.getString("twitter_id");
		return legislator;
	}
	
	
	private static Legislator legislatorFor(String url) throws CongressException {
		try {
			return fromAPI(Congress.firstResult(url));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

	private static List<Legislator> legislatorsFor(String url) throws CongressException {
		List<Legislator> legislators = new ArrayList<Legislator>();
		try {
			JSONArray results = Congress.resultsFor(url);

			int length = results.length();
			for (int i = 0; i < length; i++)
				legislators.add(fromAPI(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}

		return legislators;
	}

}