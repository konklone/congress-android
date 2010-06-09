package com.sunlightlabs.services.congress;

import java.util.ArrayList;

import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Drumbone;
import com.sunlightlabs.congress.java.Bill.Action;
import com.sunlightlabs.congress.java.Bill.Vote;
import com.sunlightlabs.services.BillService;

public class CongressBillService implements BillService {
	private CongressLegislatorService legislatorService = new CongressLegislatorService();

	public Bill fromDrumbone(JSONObject json) throws CongressException {
		Bill bill = new Bill();

		try {
			if (!json.isNull("bill_id"))
				bill.id = json.getString("bill_id");
			if (!json.isNull("code"))
				bill.code = json.getString("code");
			if (!json.isNull("type"))
				bill.type = json.getString("type");
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
				bill.last_action_at = DateUtils.parseDate(json.getString("last_action_at"),
						Drumbone.dateFormat);
			if (!json.isNull("last_vote_at"))
				bill.last_vote_at = DateUtils.parseDate(json.getString("last_vote_at"),
						Drumbone.dateFormat);

			// timeline dates
			if (!json.isNull("introduced_at"))
				bill.introduced_at = DateUtils.parseDate(json.getString("introduced_at"),
						Drumbone.dateFormat);
			if (!json.isNull("house_result_at"))
				bill.house_result_at = DateUtils.parseDate(json.getString("house_result_at"),
						Drumbone.dateFormat);
			if (!json.isNull("senate_result_at"))
				bill.senate_result_at = DateUtils.parseDate(json.getString("senate_result_at"),
						Drumbone.dateFormat);
			if (!json.isNull("passed_at"))
				bill.passed_at = DateUtils.parseDate(json.getString("passed_at"),
						Drumbone.dateFormat);
			if (!json.isNull("vetoed_at"))
				bill.vetoed_at = DateUtils.parseDate(json.getString("vetoed_at"),
						Drumbone.dateFormat);
			if (!json.isNull("override_house_result_at"))
				bill.override_house_result_at = DateUtils.parseDate(json
						.getString("override_house_result_at"), Drumbone.dateFormat);
			if (!json.isNull("override_senate_result_at"))
				bill.override_senate_result_at = DateUtils.parseDate(json
						.getString("override_senate_result_at"), Drumbone.dateFormat);
			if (!json.isNull("awaiting_signature_since"))
				bill.awaiting_signature_since = DateUtils.parseDate(json
						.getString("awaiting_signature_since"), Drumbone.dateFormat);
			if (!json.isNull("enacted_at"))
				bill.enacted_at = DateUtils.parseDate(json.getString("enacted_at"),
						Drumbone.dateFormat);

			// timeline flags and values
			if (!json.isNull("house_result"))
				bill.house_result = json.getString("house_result");
			if (!json.isNull("senate_result"))
				bill.senate_result = json.getString("senate_result");
			if (!json.isNull("passed"))
				bill.passed = json.getBoolean("passed");
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
				bill.sponsor = legislatorService.fromDrumbone(json.getJSONObject("sponsor"));

			if (!json.isNull("summary"))
				bill.summary = json.getString("summary");

			if (!json.isNull("votes")) {
				JSONArray voteObjects = json.getJSONArray("votes");
				int length = voteObjects.length();

				// load in descending order
				for (int i = 0; i < length; i++)
					bill.votes.add(0, voteFromDrumbone(voteObjects.getJSONObject(i)));

				if (!bill.votes.isEmpty()) {
					Bill.Vote vote = bill.votes.get(bill.votes.size() - 1);
					bill.last_vote_result = vote.result;
					bill.last_vote_chamber = vote.chamber;
				}
			}

			if (!json.isNull("actions")) {
				JSONArray actionObjects = json.getJSONArray("actions");
				int length = actionObjects.length();

				// load in descending order
				for (int i = 0; i < length; i++)
					bill.actions.add(0, actionFromJson(actionObjects.getJSONObject(i)));
			}
		} catch (Exception e) {
			throw new CongressException(e, "Could not parse a Bill from JSON.");
		}

		return bill;
	}

	public ArrayList<Bill> recentlyIntroduced(int n, int p) throws CongressException {
		return billsFor(Drumbone.url("bills",
				"order=introduced_at&sections=basic,sponsor&per_page=" + n + "&page=" + p));
	}

	public ArrayList<Bill> recentLaws(int n, int p) throws CongressException {
		return billsFor(Drumbone
				.url("bills", "order=enacted_at&enacted=true&sections=basic,sponsor&per_page=" + n
						+ "&page=" + p));
	}

	public ArrayList<Bill> recentlySponsored(int n, String sponsorId, int p)
			throws CongressException {
		return billsFor(Drumbone.url("bills", "order=introduced_at&sponsor_id=" + sponsorId
				+ "&sections=basic,sponsor&per_page=" + n + "&page=" + p));
	}

	public ArrayList<Bill> latestVotes(int n, int p) throws CongressException {
		return billsFor(Drumbone.url("bills",
				"order=last_vote_at&sections=basic,sponsor,votes&per_page=" + n + "&page=" + p));
	}

	public Bill find(String id, String sections) throws CongressException {
		return billFor(Drumbone.url("bill", "bill_id=" + id + "&sections=" + sections));
	}

	public Bill billFor(String url) throws CongressException {
		String rawJSON = Drumbone.fetchJSON(url);
		try {
			return fromDrumbone(new JSONObject(rawJSON).getJSONObject("bill"));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

	public ArrayList<Bill> billsFor(String url) throws CongressException {
		String rawJSON = Drumbone.fetchJSON(url);
		ArrayList<Bill> bills = new ArrayList<Bill>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("bills");

			int length = results.length();
			for (int i = 0; i < length; i++)
				bills.add(fromDrumbone(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
		return bills;
	}

	public Vote voteFromDrumbone(JSONObject json) throws CongressException {
		try {
			Vote vote = new Vote();
			vote.result = json.getString("result");
			vote.text = json.getString("text");
			vote.how = json.getString("how");
			vote.type = json.getString("type");
			vote.chamber = json.getString("chamber");
			vote.voted_at = DateUtils.parseDate(json.getString("voted_at"), Drumbone.dateFormat);

			if (!json.isNull("roll_id"))
				vote.roll_id = json.getString("roll_id");
			return vote;
		} catch (Exception e) {
			throw new CongressException(e, "Could not parse a Bill.Vote from JSON");
		}
	}

	public Action actionFromJson(JSONObject json) throws CongressException {
		try {
			Action action = new Action();
			action.text = json.getString("text");
			action.type = json.getString("type");
			action.acted_at = DateUtils.parseDate(json.getString("acted_at"), Drumbone.dateFormat);
			return action;
		} catch (Exception e) {
			throw new CongressException(e, "Could not parse a Bill.Action from JSON.");
		}
	}

}
