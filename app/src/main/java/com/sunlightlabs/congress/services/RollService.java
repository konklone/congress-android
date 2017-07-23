package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.models.Roll.Vote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RollService {
	
	public static String[] basicFields = {
		"roll_id", "chamber", "number", "year", "congress", "bill_id",
		"bill.official_title", "bill.short_title",
		"voted_on", "vote_type", "roll_type", "question", "required", "result",
		"breakdown",
        "nomination.nominees", "nomination.nomination_id", "nomination.number", "nomination.organization",
        "amendment.amendment_id", "amendment.purpose", "amendment.description", "amendment.amends_bill_id"
	};
	
	public static Roll find(String id, String[] fields) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("roll_id", id);
		
		return rollFor(Congress.url("votes", fields, params));
	}
	
	public static List<Roll> latestVotes(String bioguideId, String chamber, int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "voted_on");
		params.put("chamber", chamber);
		params.put("voter_ids." + bioguideId + "__exists", "true");
		
		String[] fields = new String[basicFields.length + 1];
		System.arraycopy(basicFields, 0, fields, 0, basicFields.length);
		fields[basicFields.length + 0] = "voter_ids." + bioguideId;

		return rollsFor(Congress.url("votes", fields, params, page, per_page)); 
	}
	
	public static List<Roll> latestVotes(int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "voted_on");
		return rollsFor(Congress.url("votes", basicFields, params, page, per_page));
	}
	
	
	/* JSON parsers, also useful for other service endpoints within this package */
	
	protected static Roll fromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		if (json == null)
			throw new CongressException("Error loading votes.");
		
		Roll roll = new Roll();
		
		if (!json.isNull("chamber"))
			roll.chamber = json.getString("chamber");
		if (!json.isNull("vote_type"))
			roll.vote_type = json.getString("vote_type");
		if (!json.isNull("question"))
			roll.question = json.getString("question");
		if (!json.isNull("result"))
			roll.result = json.getString("result");
		if (!json.isNull("congress"))
			roll.congress = json.getInt("congress");
		if (!json.isNull("year"))
			roll.year = json.getInt("year");
		if (!json.isNull("voted_on"))
			roll.voted_at = Congress.parseDate(json.getString("voted_on"));
		
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
			roll.bill = BillService.fromSunlightAPI(json.getJSONObject("bill"));

		roll.voteBreakdown.put(Roll.YEA, 0);
		roll.voteBreakdown.put(Roll.NAY, 0);
		roll.voteBreakdown.put(Roll.PRESENT, 0);
		roll.voteBreakdown.put(Roll.NOT_VOTING, 0);
		
		if (!json.isNull("breakdown")) {
			JSONObject vote_breakdown = json.getJSONObject("breakdown");
			
			JSONObject total = vote_breakdown.getJSONObject("total");
			Iterator<?> iter = total.keys();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				roll.voteBreakdown.put(key, total.getInt(key));
				if (!key.equals(Roll.YEA) && !key.equals(Roll.NAY) && !key.equals(Roll.PRESENT) && !key.equals(Roll.NOT_VOTING))
					roll.otherVotes = true;
			}
			
			//todo: what does this mean 
			// until this is fixed on the server
			if (roll.otherVotes) {
				roll.voteBreakdown.remove(Roll.YEA);
				roll.voteBreakdown.remove(Roll.NAY);
			}
		}

        if (!json.isNull("nomination")) {
            JSONObject nominationObject = json.getJSONObject("nomination");
            roll.nomination = NominationService.fromAPI(nominationObject);
        }

        if (!json.isNull("amendment")) {
            JSONObject amendmentObject = json.getJSONObject("amendment");
            roll.amendment = AmendmentService.fromAPI(amendmentObject);
        }

		if (!json.isNull("voters")) {
			roll.voters = new HashMap<String, Vote>();
			JSONObject votersObject = json.getJSONObject("voters");
			Iterator<?> iter = votersObject.keys();
			while (iter.hasNext()) {
				String voter_id = (String) iter.next();
				JSONObject voterObject = votersObject.getJSONObject(voter_id);
				
				Roll.Vote vote = voteFromAPI(voter_id, voterObject);
				
				// if there was no voter info for some reason, don't add the vote
				if (vote != null)
					roll.voters.put(voter_id, vote);
			}
		}

		if (!json.isNull("voter_ids")) {
			roll.voter_ids = new HashMap<String, Vote>();
			JSONObject voterIdsObject = json.getJSONObject("voter_ids");
			Iterator<?> iter = voterIdsObject.keys();
			while (iter.hasNext()) {
				String voter_id = (String) iter.next();
				String vote_name = voterIdsObject.getString(voter_id);
				
				roll.voter_ids.put(voter_id, voteFromAPI(voter_id, vote_name));
			}
		}

		return roll;
	}

	protected static Vote voteFromAPI(String voter_id, JSONObject json) throws JSONException, CongressException {
		Vote vote = new Vote();
		vote.vote = json.getString("vote");
		vote.voter_id = voter_id;
		vote.voter = LegislatorService.fromSunlight(json.getJSONObject("voter"));
		if (vote.voter == null)
			return null;
		else
			return vote;
	}
	
	protected static Vote voteFromAPI(String voter_id, String vote_name) throws JSONException, CongressException {
		Vote vote = new Vote();
		vote.vote = vote_name;
		vote.voter_id = voter_id;
		return vote;
	}
	
	
	private static Roll rollFor(String url) throws CongressException {
		try {
			return fromAPI(Congress.firstResult(url));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
	}
	
	private static List<Roll> rollsFor(String url) throws CongressException {
		List<Roll> rolls = new ArrayList<Roll>();
		try {
			JSONArray results = Congress.resultsFor(url);

			int length = results.length();
			for (int i = 0; i < length; i++)
				rolls.add(fromAPI(results.getJSONObject(i)));
			
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return rolls;
	}

}