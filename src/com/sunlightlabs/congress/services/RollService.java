package com.sunlightlabs.congress.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.models.Roll.Vote;

public class RollService {
	
	/* Main methods */
	
	public static Roll find(String id, String sections) throws CongressException {
		return rollFor(Drumbone.url("roll", "roll_id=" + id + "&sections=" + sections));
	}
	
	public static ArrayList<Roll> latestVotes(Legislator voter, int per_page, int page) throws CongressException {
		String query =  "per_page=" + per_page + "&page=" + page + "&order=voted_at";
		query += 		"&chamber=" + voter.chamber;
		query += 		"&sections=basic,voter_ids." + voter.bioguide_id;
		return rollsFor(Drumbone.url("rolls", query)); 
	}
	
	public static ArrayList<Roll> latestVotes(int per_page, int page) throws CongressException {
		String query =  "per_page=" + per_page + "&page=" + page + "&order=voted_at";
		query += 		"&sections=basic";
		return rollsFor(Drumbone.url("rolls", query));
	}
	
	/* JSON parsers, also useful for other service endpoints within this package */
	
	protected static Roll fromDrumbone(JSONObject json) throws JSONException, DateParseException {
		Roll roll = new Roll();
		
		if (!json.isNull("roll_id"))
			roll.id = json.getString("roll_id");
		if (!json.isNull("chamber"))
			roll.chamber = json.getString("chamber");
		if (!json.isNull("type"))
			roll.type = json.getString("type");
		if (!json.isNull("question"))
			roll.question = json.getString("question");
		if (!json.isNull("result"))
			roll.result = json.getString("result");
		if (!json.isNull("bill_id"))
			roll.bill_id = json.getString("bill_id");
		if (!json.isNull("required"))
			roll.required = json.getString("required");
		if (!json.isNull("number"))
			roll.number = json.getInt("number");
		if (!json.isNull("session"))
			roll.session = json.getInt("session");
		if (!json.isNull("year"))
			roll.year = json.getInt("year");
		if (!json.isNull("voted_at"))
			roll.voted_at = DateUtils.parseDate(json.getString("voted_at"), Drumbone.dateFormat);

		if (!json.isNull("bill"))
			roll.bill = BillService.fromDrumbone(json.getJSONObject("bill"));

		if (!json.isNull("vote_breakdown")) {
			JSONObject vote_breakdown = json.getJSONObject("vote_breakdown");
			Iterator<?> iter = vote_breakdown.keys();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				if (key.equals("ayes")) // yeah, I made a mistake in the key name in Drumbone :( 
					roll.yeas = vote_breakdown.getInt(key);
				else if (key.equals("nays"))
					roll.nays = vote_breakdown.getInt(key);
				else if (key.equals("present"))
					roll.present = vote_breakdown.getInt(key);
				else if (key.equals("not_voting"))
					roll.not_voting = vote_breakdown.getInt(key);
				else
					roll.otherVotes.put(key, vote_breakdown.getInt(key));
			}
		}

		if (!json.isNull("voters")) {
			roll.voters = new HashMap<String, Vote>();
			JSONObject votersObject = json.getJSONObject("voters");
			Iterator<?> iter = votersObject.keys();
			while (iter.hasNext()) {
				String voter_id = (String) iter.next();
				Vote vote = voteFromDrumbone(votersObject.getJSONObject(voter_id));
				vote.voter_id = voter_id;
				roll.voters.put(voter_id, vote);
			}
		}

		if (!json.isNull("voter_ids")) {
			roll.voter_ids = new HashMap<String, Vote>();
			JSONObject voterIdsObject = json.getJSONObject("voter_ids");
			Iterator<?> iter = voterIdsObject.keys();
			while (iter.hasNext()) {
				String voter_id = (String) iter.next();
				roll.voter_ids.put(voter_id, new Vote(voter_id, voterIdsObject.getString(voter_id)));
			}
		}

		return roll;
	}

	protected static Vote voteFromDrumbone(JSONObject json) throws JSONException {
		Vote vote = new Vote();
		vote.vote_name = json.getString("vote");
		vote.vote = Roll.voteForName(vote.vote_name);
		vote.voter = LegislatorService.fromDrumbone(json.getJSONObject("voter"));
		return vote;
	}
	
	
	/* Private helpers for loading single or plural bill objects */
		
	private static Roll rollFor(String url) throws CongressException {
		String rawJSON = Drumbone.fetchJSON(url);
		try {
			return fromDrumbone(new JSONObject(rawJSON).getJSONObject("roll"));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (DateParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
	}
	
	private static ArrayList<Roll> rollsFor(String url) throws CongressException {
		String rawJSON = Drumbone.fetchJSON(url);
		ArrayList<Roll> rolls = new ArrayList<Roll>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("rolls");

			int length = results.length();
			for (int i = 0; i < length; i++)
				rolls.add(fromDrumbone(results.getJSONObject(i)));
			
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (DateParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return rolls;
	}

}