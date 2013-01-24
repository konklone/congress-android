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
		params.put("order", "name__asc");
		return committeesFor(Congress.url("committees", fields, params, 1, Congress.MAX_PER_PAGE));
	}
	
	public static List<Committee> getAll(String chamber) throws CongressException {
		String[] fields = new String[] {"committee_id", "chamber", "name"};
		Map<String,String> params = new HashMap<String,String>();
		params.put("chamber", chamber);
		params.put("subcommittee", "false");
		params.put("order", "name__asc");
		return committeesFor(Congress.url("committees", fields, params, 1, Congress.MAX_PER_PAGE));
	}
	
	protected static Committee fromAPI(JSONObject json) throws JSONException, CongressException {
		Committee committee = new Committee();
		committee.id = json.getString("committee_id");
		committee.name = Congress.unicode(json.getString("name"));
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
	
	
	private static Committee committeeFor(String url) throws CongressException {
		try {
			return fromAPI(Congress.firstResult(url));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

	private static List<Committee> committeesFor(String url) throws CongressException {
		List<Committee> committees = new ArrayList<Committee>();
		try {
			JSONArray results = Congress.resultsFor(url);

			int length = results.length();
			for (int i = 0; i < length; i++)
				committees.add(fromAPI(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}

		return committees;
	}

}