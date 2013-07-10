package com.sunlightlabs.congress.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import com.sunlightlabs.congress.models.UpcomingBill;

public class BillService {
	
	public static String[] basicFields = {
		"bill_id", "bill_type", "chamber", "number", "congress",
		"introduced_on", "last_action_at", "last_vote_at",
		"official_title", "short_title", "cosponsors_count",
		"urls", "last_version.urls",
		"history", 
		"sponsor",
		"upcoming",
		"last_action"
	};
	
	public static List<Bill> recentlyIntroduced(int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "introduced_on,bill_type,number");
		return billsFor(Congress.url("bills", basicFields, params, page, per_page)); 
	}
	
	public static List<Bill> recentlyActive(int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "last_action_at");
		params.put("history.active", "true");
		
		return billsFor(Congress.url("bills", basicFields, params, page, per_page)); 
	}

	public static List<Bill> recentlySponsored(String sponsorId, int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "introduced_on,bill_type,number");
		params.put("sponsor_id", sponsorId);
		return billsFor(Congress.url("bills", basicFields, params, page, per_page));
	}
	
	public static List<Bill> search(String query, Map<String,String> params, int page, int per_page) throws CongressException {
		return billsFor(Congress.searchUrl("bills", query, true, basicFields, params, page, per_page));
	}
	
	public static List<Bill> where(Map<String,String> params, int page, int per_page) throws CongressException {
		if (!params.containsKey("order"))
			params.put("order", "introduced_on,bill_type,number");
		
		return billsFor(Congress.url("bills", basicFields, params, page, per_page));
	}

	public static Bill find(String id, String[] fields) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("bill_id", id);
		return billFor(Congress.url("bills", fields, params));
	}
	
	protected static Bill fromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		Bill bill = new Bill();

		if (!json.isNull("bill_id"))
			bill.id = json.getString("bill_id");
		
		if (!json.isNull("bill_type"))
			bill.bill_type = json.getString("bill_type");
		
		if (!json.isNull("chamber"))
			bill.chamber = json.getString("chamber");
		
		// todo: rename field
		if (!json.isNull("congress"))
			bill.congress = json.getInt("congress");
		
		if (!json.isNull("number"))
			bill.number = json.getInt("number");
		
		if (!json.isNull("short_title"))
			bill.short_title = json.getString("short_title");
		if (!json.isNull("official_title"))
			bill.official_title = json.getString("official_title");
		if (!json.isNull("last_action_at"))
			bill.last_action_at = Congress.parseDateEither(json.getString("last_action_at"));
		if (!json.isNull("last_vote_at"))
			bill.last_passage_vote_at = Congress.parseDateEither(json.getString("last_vote_at"));
		
		if (!json.isNull("introduced_on"))
			bill.introduced_on = Congress.parseDateOnly(json.getString("introduced_on"));
		
		if (!json.isNull("cosponsors_count"))
			bill.cosponsors_count = json.getInt("cosponsors_count");
		
		// timeline dates
		if (!json.isNull("history")) {
			JSONObject history = json.getJSONObject("history");
			if (!history.isNull("active_at"))
				bill.active_at = Congress.parseDateEither(history.getString("active_at"));
			if (!history.isNull("senate_cloture_result_at"))
				bill.senate_cloture_result_at = Congress.parseDateEither(history.getString("senate_cloture_result_at"));
			if (!history.isNull("house_passage_result_at"))
				bill.house_passage_result_at = Congress.parseDateEither(history.getString("house_passage_result_at"));
			if (!history.isNull("senate_passage_result_at"))
				bill.senate_passage_result_at = Congress.parseDateEither(history.getString("senate_passage_result_at"));
			if (!history.isNull("vetoed_at"))
				bill.vetoed_at = Congress.parseDateEither(history.getString("vetoed_at"));
			if (!history.isNull("house_override_result_at"))
				bill.house_override_result_at = Congress.parseDateEither(history.getString("house_override_result_at"));
			if (!history.isNull("senate_override_result_at"))
				bill.senate_override_result_at = Congress.parseDateEither(history.getString("senate_override_result_at"));
			if (!history.isNull("awaiting_signature_since"))
				bill.awaiting_signature_since = Congress.parseDateEither(history.getString("awaiting_signature_since"));
			if (!history.isNull("enacted_at"))
				bill.enacted_at = Congress.parseDateEither(history.getString("enacted_at"));
	
			// timeline flags and values
			if (!history.isNull("active"))
				bill.active = history.getBoolean("active");
			if (!history.isNull("senate_cloture_result"))
				bill.senate_cloture_result = history.getString("senate_cloture_result");
			if (!history.isNull("house_passage_result"))
				bill.house_passage_result = history.getString("house_passage_result");
			if (!history.isNull("senate_passage_result"))
				bill.senate_passage_result = history.getString("senate_passage_result");
			if (!history.isNull("vetoed"))
				bill.vetoed = history.getBoolean("vetoed");
			if (!history.isNull("house_override_result"))
				bill.house_override_result = history.getString("house_override_result");
			if (!history.isNull("senate_override_result"))
				bill.senate_override_result = history.getString("senate_override_result");
			if (!history.isNull("awaiting_signature"))
				bill.awaiting_signature = history.getBoolean("awaiting_signature");
			if (!history.isNull("enacted"))
				bill.enacted = history.getBoolean("enacted");
		}

		if (!json.isNull("sponsor"))
			bill.sponsor = LegislatorService.fromAPI(json.getJSONObject("sponsor"));

		if (!json.isNull("summary"))
			bill.summary = json.getString("summary");

		if (!json.isNull("cosponsors")) {
			JSONArray cosponsorObjects = json.getJSONArray("cosponsors");
			int length = cosponsorObjects.length();
			
			bill.cosponsors = new ArrayList<Legislator>();
			
			for (int i=0; i<length; i++)
				bill.cosponsors.add(LegislatorService.fromAPI(cosponsorObjects.getJSONObject(i).getJSONObject("legislator")));
		}
		
		if (!json.isNull("votes")) {
			JSONArray voteObjects = json.getJSONArray("votes");
			int length = voteObjects.length();
			
			bill.votes = new ArrayList<Bill.Vote>();

			// load in descending order
			for (int i = 0; i < length; i++)
				bill.votes.add(0, voteFromAPI(voteObjects.getJSONObject(i)));
		}

		if (!json.isNull("actions")) {
			JSONArray actionObjects = json.getJSONArray("actions");
			int length = actionObjects.length();

			bill.actions = new ArrayList<Bill.Action>();
			
			// load in descending order
			for (int i = 0; i < length; i++)
				bill.actions.add(0, actionFromAPI(actionObjects.getJSONObject(i)));
		}
		
		if (!json.isNull("last_action"))
			bill.lastAction = actionFromAPI(json.getJSONObject("last_action"));
		
		if (!json.isNull("upcoming")) {
			JSONArray upcomingObjects = json.getJSONArray("upcoming");
			int length = upcomingObjects.length();
			
			List<UpcomingBill> upcoming = new ArrayList<UpcomingBill>();
			
			for (int i = 0; i < length; i++)
				upcoming.add(UpcomingBillService.fromAPI(upcomingObjects.getJSONObject(i)));
			
			// sort in order of legislative day
			Collections.sort(upcoming, new Comparator<UpcomingBill>() {
				@Override
				public int compare(UpcomingBill a, UpcomingBill b) {
					return a.legislativeDay.compareTo(b.legislativeDay);
				}
			});
			
			bill.upcoming = upcoming;
		}
		
		if (!json.isNull("last_version")) {
			JSONObject version = json.getJSONObject("last_version");
			if (!version.isNull("urls")) {
				bill.versionUrls = new HashMap<String,String>();
				JSONObject urls = version.getJSONObject("urls");
				if (!urls.isNull("html"))
					bill.versionUrls.put("html", urls.getString("html"));
				if (!urls.isNull("xml"))
					bill.versionUrls.put("xml", urls.getString("xml"));
				if (!urls.isNull("pdf"))
					bill.versionUrls.put("pdf", urls.getString("pdf"));
			}
		}
		
		if (!json.isNull("urls")) {
			bill.urls = new HashMap<String,String>();
			JSONObject urls = json.getJSONObject("urls");
			if (!urls.isNull("congress"))
				bill.urls.put("congress", urls.getString("congress"));
			if (!urls.isNull("govtrack"))
				bill.urls.put("govtrack", urls.getString("govtrack"));
			if (!urls.isNull("opencongress"))
				bill.urls.put("opencongress", urls.getString("opencongress"));
		}
		
		// coming from a search endpoint, generate a search object
		if (!json.isNull("search"))
			bill.search = Congress.SearchResult.from(json.getJSONObject("search"));
		
		return bill;
	}
	
	protected static Vote voteFromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		Vote vote = new Vote();
		
		vote.result = json.getString("result");
		vote.text = json.getString("text");
		vote.how = json.getString("how");
		vote.passage_type = json.getString("vote_type");
		vote.chamber = json.getString("chamber");
		vote.voted_at = Congress.parseDateEither(json.getString("acted_at"));

		if (!json.isNull("roll_id"))
			vote.roll_id = json.getString("roll_id");
		return vote;
	}
	
	protected static Action actionFromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		Action action = new Action();
		action.text = json.getString("text");
		action.type = json.getString("type");
		action.acted_at = Congress.parseDateEither(json.getString("acted_at"));
		
		if (!json.isNull("chamber"))
			action.chamber = json.getString("chamber");
		if (!json.isNull("result"))
			action.result = json.getString("result");
		
		return action;
	}
	
	
	/* Private helpers for loading single or plural bill objects */

	private static Bill billFor(String url) throws CongressException {
		try {
			JSONObject json = Congress.firstResult(url);
			if (json != null)
				return fromAPI(json);
			else
				throw new BillNotFoundException();
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
	}

	private static List<Bill> billsFor(String url) throws CongressException {
		List<Bill> bills = new ArrayList<Bill>();
		try {
			JSONArray results = Congress.resultsFor(url);

			int length = results.length();
			for (int i = 0; i < length; i++)
				bills.add(fromAPI(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return bills;
	}
	
	public static class BillNotFoundException extends CongressException {
		private static final long serialVersionUID = 1L;

		public BillNotFoundException() {
			super("No bill found by that ID.");
		}
	}
}
