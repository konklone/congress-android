package com.sunlightlabs.congress.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class CommitteeService {
	
	/* Main methods */
	
	public static Committee find(String id) throws CongressException {
		String[] fields = new String[] {"committee_id", "chamber", "name", "members"};
		Map<String,String> params = new HashMap<String,String>();
		params.put("committee_id", id);
		return committeeFor(Congress.url("committees", fields, params));
	}

	public static List<Committee> forLegislator(String bioguideId) throws CongressException {
		String[] fields = new String[] {"committee_id", "chamber", "name"};
		Map<String,String> params = new HashMap<String,String>();
		params.put("member_ids", bioguideId);
		params.put("subcommittee", "false");
		return committeesFor(Congress.url("committees", fields, params, 1, Congress.MAX_PER_PAGE));
	}
	
	public static List<Committee> getAll(String chamber) throws CongressException {
		String[] fields = new String[] {"committee_id", "chamber", "name"};
		Map<String,String> params = new HashMap<String,String>();
		params.put("chamber", chamber);
		params.put("subcommittee", "false");
		return committeesFor(Congress.url("committees", fields, params, 1, Congress.MAX_PER_PAGE));
	}
	
	/* JSON parsers, also useful for other service endpoints within this package */
	
	protected static Committee fromAPI(JSONObject json) throws JSONException {
		Committee committee = new Committee();
		committee.id = json.getString("committee_id");
		committee.name = json.getString("name");
		committee.chamber = json.getString("chamber");

		if (!json.isNull("members")) {
			committee.members = new ArrayList<Legislator>();
			committee.memberships = new ArrayList<Committee.Membership>();
			
			JSONArray memberList = json.getJSONArray("members");
			for (int i = 0; i < memberList.length(); i++) {
				JSONObject memberJson = memberList.getJSONObject(i);
				Legislator legislator = LegislatorService.fromAPI(memberJson.getJSONObject("legislator"));
				
				Committee.Membership membership = new Committee.Membership();
				if (!memberJson.isNull("side"))
					membership.side = memberJson.getString("side");
				if (!memberJson.isNull("rank"))
					membership.rank = memberJson.getString("rank");
				if (!memberJson.isNull("title"))
					membership.title = memberJson.getString("title");
						
				committee.members.add(legislator);
				committee.memberships.add(membership);
			}
		}
		
		return committee;
	}
	
	
	/* Private helpers for loading single or plural bill objects */

	private static Committee committeeFor(String url) throws CongressException {
		String rawJSON = Congress.fetchJSON(url);
		try {
			JSONArray committees = new JSONObject(rawJSON).getJSONArray("results");
			if (committees.length() > 0)
				return fromAPI((JSONObject) committees.get(0));
			else
				return null;
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

	private static List<Committee> committeesFor(String url) throws CongressException {
		String rawJSON = Congress.fetchJSON(url);
		List<Committee> committees = new ArrayList<Committee>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("results");

			int length = results.length();
			for (int i = 0; i < length; i++)
				committees.add(fromAPI(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}

		return committees;
	}

}