package com.sunlightlabs.services.congress;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.java.Committee;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Legislator;
import com.sunlightlabs.congress.java.Sunlight;
import com.sunlightlabs.services.CommitteeService;

public class CongressCommitteeService implements CommitteeService {
	private CongressLegislatorService legislatorService = new CongressLegislatorService();

	public Committee fromSunlight(JSONObject json) throws CongressException {
		try {
			Committee committee = new Committee();
			committee.id = json.getString("id");
			committee.name = json.getString("name");
			committee.chamber = json.getString("chamber");

			if (!json.isNull("members")) {
				committee.members = new ArrayList<Legislator>();
				JSONArray memberList = json.getJSONArray("members");
				for (int i = 0; i < memberList.length(); i++)
					committee.members.add(legislatorService.fromSunlight(memberList
							.getJSONObject(i).getJSONObject("legislator")));
			}
			return committee;
		} catch (JSONException e) {
			throw new CongressException(e, "Could not parse a Committee from JSON.");
		}
	}

	public Committee find(String id) throws CongressException {
		return committeeFor(Sunlight.url("committees.get", "id=" + id));
	}

	public ArrayList<Committee> forLegislator(String bioguideId) throws CongressException {
		return committeesFor(Sunlight.url("committees.allForLegislator", "bioguide_id="
				+ bioguideId));
	}

	public Committee committeeFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		try {
			return fromSunlight(new JSONObject(rawJSON).getJSONObject("response").getJSONObject(
					"committee"));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

	public ArrayList<Committee> committeesFor(String url) throws CongressException {
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
