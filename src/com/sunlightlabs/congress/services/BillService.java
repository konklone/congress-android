package com.sunlightlabs.congress.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.Bill.Action;
import com.sunlightlabs.congress.models.Bill.Vote;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class BillService {
	
	/* Main methods */
	
	public static List<Bill> recentlyIntroduced(int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "introduced_at");
		
		String[] sections = new String[] {"basic", "sponsor"};
		
		return billsFor(RealTimeCongress.url("bills", sections, params, page, per_page)); 
	}

	public static List<Bill> recentLaws(int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "enacted_at");
		params.put("enacted", "true");
		
		String[] sections = new String[] {"basic", "sponsor"};
		
		return billsFor(RealTimeCongress.url("bills", sections, params, page, per_page));
	}

	public static List<Bill> recentlySponsored(String sponsorId, int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "introduced_at");
		params.put("sponsor_id", sponsorId);
		
		String[] sections = new String[] {"basic", "sponsor"};
		
		return billsFor(RealTimeCongress.url("bills", sections, params, page, per_page));
	}

	public static Bill find(String id, String[] sections) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("bill_id", id);
				
		return billFor(RealTimeCongress.url("bills", sections, params));
	}
	
	/* JSON parsers, also useful for other service endpoints within this package */

	protected static Bill fromRTC(JSONObject json) throws JSONException, ParseException {
		Bill bill = new Bill();

		if (!json.isNull("bill_id"))
			bill.id = json.getString("bill_id");
		if (!json.isNull("code"))
			bill.code = json.getString("code");
		if (!json.isNull("bill_type"))
			bill.bill_type = json.getString("bill_type");
		if (!json.isNull("state"))
			bill.state = json.getString("state");
		if (!json.isNull("chamber"))
			bill.chamber = json.getString("chamber");
		if (!json.isNull("session"))
			bill.session = json.getInt("session");
		if (!json.isNull("number"))
			bill.number = json.getInt("number");

		if (!json.isNull("short_title"))
			bill.short_title = json.getString("short_title");
		if (!json.isNull("official_title"))
			bill.official_title = json.getString("official_title");
		if (!json.isNull("last_action_at"))
			bill.last_action_at = RealTimeCongress.parseDate(json.getString("last_action_at"));
		if (!json.isNull("last_passage_vote_at"))
			bill.last_passage_vote_at = RealTimeCongress.parseDate(json.getString("last_passage_vote_at"));
		if (!json.isNull("cosponsors_count"))
			bill.cosponsors_count = json.getInt("cosponsors_count");

		// timeline dates
		if (!json.isNull("introduced_at"))
			bill.introduced_at = RealTimeCongress.parseDate(json.getString("introduced_at"));
		if (!json.isNull("house_result_at"))
			bill.house_result_at = RealTimeCongress.parseDate(json.getString("house_result_at"));
		if (!json.isNull("senate_result_at"))
			bill.senate_result_at = RealTimeCongress.parseDate(json.getString("senate_result_at"));
		if (!json.isNull("vetoed_at"))
			bill.vetoed_at = RealTimeCongress.parseDate(json.getString("vetoed_at"));
		if (!json.isNull("override_house_result_at"))
			bill.override_house_result_at = RealTimeCongress.parseDate(json.getString("override_house_result_at"));
		if (!json.isNull("override_senate_result_at"))
			bill.override_senate_result_at = RealTimeCongress.parseDate(json.getString("override_senate_result_at"));
		if (!json.isNull("awaiting_signature_since"))
			bill.awaiting_signature_since = RealTimeCongress.parseDate(json.getString("awaiting_signature_since"));
		if (!json.isNull("enacted_at"))
			bill.enacted_at = RealTimeCongress.parseDate(json.getString("enacted_at"));

		// timeline flags and values
		if (!json.isNull("house_result"))
			bill.house_result = json.getString("house_result");
		if (!json.isNull("senate_result"))
			bill.senate_result = json.getString("senate_result");
		if (!json.isNull("vetoed"))
			bill.vetoed = json.getBoolean("vetoed");
		if (!json.isNull("override_house_result"))
			bill.override_house_result = json.getString("override_house_result");
		if (!json.isNull("override_senate_result"))
			bill.override_senate_result = json.getString("override_senate_result");
		if (!json.isNull("awaiting_signature"))
			bill.awaiting_signature = json.getBoolean("awaiting_signature");
		if (!json.isNull("enacted"))
			bill.enacted = json.getBoolean("enacted");

		if (!json.isNull("sponsor"))
			bill.sponsor = LegislatorService.fromRTC(json.getJSONObject("sponsor"));

		if (!json.isNull("summary"))
			bill.summary = json.getString("summary");

		if (!json.isNull("cosponsors")) {
			JSONArray cosponsorObjects = json.getJSONArray("cosponsors");
			int length = cosponsorObjects.length();
			
			bill.cosponsors = new ArrayList<Legislator>();
			
			for (int i=0; i<length; i++)
				bill.cosponsors.add(LegislatorService.fromRTC(cosponsorObjects.getJSONObject(i)));
		}
		
		if (!json.isNull("passage_votes")) {
			JSONArray voteObjects = json.getJSONArray("votes");
			int length = voteObjects.length();
			
			bill.passage_votes = new ArrayList<Bill.Vote>();

			// load in descending order
			for (int i = 0; i < length; i++)
				bill.passage_votes.add(0, voteFromRTC(voteObjects.getJSONObject(i)));

			if (!bill.passage_votes.isEmpty()) {
				Bill.Vote vote = bill.passage_votes.get(bill.passage_votes.size() - 1);
				bill.last_vote_result = vote.result;
				bill.last_vote_chamber = vote.chamber;
			}
		}

		if (!json.isNull("actions")) {
			JSONArray actionObjects = json.getJSONArray("actions");
			int length = actionObjects.length();

			bill.actions = new ArrayList<Bill.Action>();
			
			// load in descending order
			for (int i = 0; i < length; i++)
				bill.actions.add(0, actionFromRTC(actionObjects.getJSONObject(i)));
		}
		
		return bill;
	}
	
	protected static Vote voteFromRTC(JSONObject json) throws JSONException, ParseException {
		Vote vote = new Vote();
		
		vote.result = json.getString("result");
		vote.text = json.getString("text");
		vote.how = json.getString("how");
		vote.passage_type = json.getString("passage_type");
		vote.chamber = json.getString("chamber");
		vote.voted_at = RealTimeCongress.parseDate(json.getString("voted_at"));

		if (!json.isNull("roll_id"))
			vote.roll_id = json.getString("roll_id");
		return vote;
	}

	protected static Action actionFromRTC(JSONObject json) throws JSONException, ParseException {
		Action action = new Action();
		action.text = json.getString("text");
		action.type = json.getString("type");
		action.acted_at = RealTimeCongress.parseDate(json.getString("acted_at"));
		return action;
	}
	
	
	/* Private helpers for loading single or plural bill objects */

	private static Bill billFor(String url) throws CongressException {
		String rawJSON = RealTimeCongress.fetchJSON(url);
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("bills");
			if (results.length() == 0)
				throw new CongressException("Bill not found.");
			else
				return fromRTC(results.getJSONObject(0));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
	}

	private static List<Bill> billsFor(String url) throws CongressException {
		String rawJSON = RealTimeCongress.fetchJSON(url);
		List<Bill> bills = new ArrayList<Bill>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("bills");

			int length = results.length();
			for (int i = 0; i < length; i++)
				bills.add(fromRTC(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return bills;
	}
}
