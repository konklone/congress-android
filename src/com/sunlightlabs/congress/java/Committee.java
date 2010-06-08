package com.sunlightlabs.congress.java;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.java.service.Services;

public class Committee implements Comparable<Committee> {

	public String id, name, chamber;
	public ArrayList<Legislator> members;
	
	public static ArrayList<Committee> forLegislator(String bioguide_id) throws CongressException {
		return committeesFor(Sunlight.url("committees.allForLegislator", "bioguide_id=" + bioguide_id));
	}
	
	public static Committee find(String id) throws CongressException {
		return committeeFor(Sunlight.url("committees.get", "id=" + id));
	}
	
	public Committee(JSONObject json) throws JSONException, CongressException {
		id = json.getString("id");
		name = json.getString("name");
		chamber = json.getString("chamber");
		
		if (!json.isNull("members")) {
			members = new ArrayList<Legislator>();
			JSONArray memberList = json.getJSONArray("members");
			for (int i=0; i<memberList.length(); i++)
				members.add(Services.legislator.fromSunlight(memberList.getJSONObject(i)
						.getJSONObject("legislator")));
		}
	}
	
	private static Committee committeeFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		try {
			return new Committee(new JSONObject(rawJSON).getJSONObject("response").getJSONObject("committee"));
		} catch(JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}
	
	private static ArrayList<Committee> committeesFor(String url) throws CongressException {
		String rawJSON = Sunlight.fetchJSON(url);
		ArrayList<Committee> committees = new ArrayList<Committee>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONObject("response").getJSONArray("committees");
			
			int length = results.length();
			for (int i = 0; i<length; i++)
				committees.add(new Committee(results.getJSONObject(i).getJSONObject("committee")));
				
		} catch(JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
		
		return committees;
	}
	
	public int compareTo(Committee another) {
		return this.name.compareTo(another.name);
	}
	
	public String toString() {
		return name;
	}
	
}