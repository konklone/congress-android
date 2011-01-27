package com.sunlightlabs.congress.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.models.Roll.Vote;

public class RollService {
	
	/* Main methods */
	
	public static Roll find(String id, String[] sections) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("roll_id", id);
		params.put("how", "roll");
		
		return rollFor(RealTimeCongress.url("votes", sections, params));
	}
	
	public static List<Roll> latestVotes(String bioguideId, String chamber, int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "voted_at");
		params.put("chamber", chamber);
		params.put("how", "roll");
		
		String[] sections = new String[] {"basic", "voter_ids." + bioguideId};
		
		return rollsFor(RealTimeCongress.url("votes", sections, params, page, per_page)); 
	}
	
	public static List<Roll> latestVotes(int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "voted_at");
		params.put("how", "roll");
		
		String[] sections = new String[] {"basic"};
		
		return rollsFor(RealTimeCongress.url("votes", sections, params, page, per_page));
	}
	
	public static List<Roll> latestNominations(int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "voted_at");
		params.put("chamber", "senate");
		params.put("vote_type", "nomination");
		params.put("how", "roll");
		
		String[] sections = new String[] {"basic"};
		
		return rollsFor(RealTimeCongress.url("votes", sections, params, page, per_page));
	}
	
	/* JSON parsers, also useful for other service endpoints within this package */
	
	protected static Roll fromRTC(JSONObject json) throws JSONException, ParseException {
		Roll roll = new Roll();
		
		// guaranteed fields for all votes
		if (!json.isNull("how"))
			roll.how = json.getString("how");
		if (!json.isNull("chamber"))
			roll.chamber = json.getString("chamber");
		if (!json.isNull("vote_type"))
			roll.vote_type = json.getString("vote_type");
		if (!json.isNull("question"))
			roll.question = json.getString("question");
		if (!json.isNull("result"))
			roll.result = json.getString("result");
		if (!json.isNull("session"))
			roll.session = json.getInt("session");
		if (!json.isNull("year"))
			roll.year = json.getInt("year");
		if (!json.isNull("voted_at"))
			roll.voted_at = RealTimeCongress.parseDate(json.getString("voted_at"));
		
		// guaranteed fields for roll call votes
		if (!json.isNull("required"))
			roll.required = json.getString("required");
		if (!json.isNull("number"))
			roll.number = json.getInt("number");
		if (!json.isNull("roll_id"))
			roll.id = json.getString("roll_id");
		if (!json.isNull("roll_type"))
			roll.roll_type = json.getString("roll_type");
		
		
		if (!json.isNull("bill_id"))
			roll.bill_id = json.getString("bill_id");

		if (!json.isNull("bill"))
			roll.bill = BillService.fromRTC(json.getJSONObject("bill"));

		if (!json.isNull("vote_breakdown")) {
			JSONObject vote_breakdown = json.getJSONObject("vote_breakdown");
			
			JSONObject total = vote_breakdown.getJSONObject("total");
			Iterator<?> iter = total.keys();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				roll.voteBreakdown.put(key, total.getInt(key));
				if (!key.equals(Roll.YEA) && !key.equals(Roll.NAY) && !key.equals(Roll.PRESENT) && !key.equals(Roll.NOT_VOTING))
					roll.otherVotes = true;
			}
			
			// until this is fixed on the server
			if (roll.otherVotes) {
				roll.voteBreakdown.remove(Roll.YEA);
				roll.voteBreakdown.remove(Roll.NAY);
			}
		}

		if (!json.isNull("voters")) {
			roll.voters = new HashMap<String, Vote>();
			JSONObject votersObject = json.getJSONObject("voters");
			Iterator<?> iter = votersObject.keys();
			while (iter.hasNext()) {
				String voter_id = (String) iter.next();
				JSONObject voterObject = votersObject.getJSONObject(voter_id);
				
				roll.voters.put(voter_id, voteFromRTC(voter_id, voterObject));
			}
		}

		if (!json.isNull("voter_ids")) {
			roll.voter_ids = new HashMap<String, Vote>();
			JSONObject voterIdsObject = json.getJSONObject("voter_ids");
			Iterator<?> iter = voterIdsObject.keys();
			while (iter.hasNext()) {
				String voter_id = (String) iter.next();
				String vote_name = voterIdsObject.getString(voter_id);
				
				roll.voter_ids.put(voter_id, voteFromRTC(voter_id, vote_name));
			}
		}

		return roll;
	}

	protected static Vote voteFromRTC(String voter_id, JSONObject json) throws JSONException {
		Vote vote = new Vote();
		vote.vote = json.getString("vote");
		vote.voter_id = voter_id;
		vote.voter = LegislatorService.fromRTC(json.getJSONObject("voter"));
		return vote;
	}
	
	protected static Vote voteFromRTC(String voter_id, String vote_name) throws JSONException {
		Vote vote = new Vote();
		vote.vote = vote_name;
		vote.voter_id = voter_id;
		return vote;
	}
	
	
	/* Private helpers for loading single or plural bill objects */
		
	private static Roll rollFor(String url) throws CongressException {
		String rawJSON = RealTimeCongress.fetchJSON(url);
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("votes");
			if (results.length() == 0)
				throw new CongressException("Vote not found.");
			else
				return fromRTC(results.getJSONObject(0));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
	}
	
	private static List<Roll> rollsFor(String url) throws CongressException {
		String rawJSON = RealTimeCongress.fetchJSON(url);
		List<Roll> rolls = new ArrayList<Roll>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("votes");

			int length = results.length();
			for (int i = 0; i < length; i++)
				rolls.add(fromRTC(results.getJSONObject(i)));
			
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return rolls;
	}

}