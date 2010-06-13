package com.sunlightlabs.congress.services;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class CommitteeService {
	
	/* Main methods */
	
	public static Committee find(String id) throws CongressException {
		return committeeFor(Sunlight.url("committees.get", "id=" + id));
	}

	public static ArrayList<Committee> forLegislator(String bioguideId) throws CongressException {
		return committeesFor(Sunlight.url("committees.allForLegislator", "bioguide_id="
				+ bioguideId));
	}
	
	
	/* JSON parsers, also useful for other service endpoints within this package */
	
	protected static Committee fromSunlight(JSONObject json) throws JSONException {
		Committee committee = new Committee();
		committee.id = json.getString("id");
		committee.name = json.getString("name");
		committee.chamber = json.getString("chamber");

		if (!json.isNull("members")) {
			committee.members = new ArrayList<Legislator>();
			JSONArray memberList = json.getJSONArray("members");
			for (int i = 0; i < memberList.length(); i++)
				committee.members.add(LegislatorService.fromSunlight(memberList.getJSONObject(i).getJSONObject("legislator")));
		}
		
		return committee;
	}
	
	
	/* Private helpers for loading single or plural bill objects */

	private static Committee committeeFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		try {
			return fromSunlight(new JSONObject(rawJSON).getJSONObject("response").getJSONObject(
					"committee"));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

	private static ArrayList<Committee> committeesFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		ArrayList<Committee> committees = new ArrayList<Committee>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONObject("response").getJSONArray(
					"committees");

			int length = results.length();
			for (int i = 0; i < length; i++)
				committees.add(fromSunlight(results.getJSONObject(i).getJSONObject("committee")));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}

		return committees;
	}

}