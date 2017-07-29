package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillService {

    // /{congress}/both/bills/{type}.json
    public static List<Bill> recentlyIntroduced(int page) throws CongressException {
        return recently("introduced", page);
    }

    public static List<Bill> recentlyActive(int page) throws CongressException {
        return recently("active", page);
    }

    public static List<Bill> recentlyLaw(int page) throws CongressException {
        return recently("enacted", page);
    }

	public static List<Bill> recently(String type, int page) throws CongressException {
        String congress = String.valueOf(Bill.currentCongress());
        String[] both = { congress, "both", "bills", type };
        return billsFor(ProPublica.url(both, page));
	}

    // /members/{member-id}/bills/introduced.json
	public static List<Bill> recentlySponsored(String sponsorId, int page) throws CongressException {
		String[] endpoint = { "members", sponsorId, "bills", "introduced" };
        return billsFor(ProPublica.url(endpoint, page));
	}

	// /bills/search.json?query={query}&sort=date&dir=desc
	public static List<Bill> searchLatest(String query, int page) throws CongressException {
        return search(query, "date", page);
	}

    // /bills/search.json?query={query}&sort=_score&dir=desc
    public static List<Bill> searchRelevant(String query, int page) throws CongressException {
        return search(query, "_score", page);
    }

    // /bills/search.json?query={query}&sort={sort}&dir=desc
    public static List<Bill> search(String query, String sort, int page) throws CongressException {
        String[] endpoint = { "bills", "search" };

        String quoted = "\"" + query + "\"";
        Map<String,String> params = new HashMap<String,String>();
        params.put("query", quoted);
        params.put("sort", sort);
        params.put("dir", "desc");

        return billsFor(ProPublica.url(endpoint, params, page));
	}

    // /{congress}/bills/{bill_type+bill_number}.json
	public static Bill find(String id) throws CongressException {
        String[] pieces = Bill.splitBillId(id);
        String typeNumber = pieces[0] + pieces[1];
        String[] endpoint = { String.valueOf(Bill.currentCongress()), "bills", typeNumber };
		return billFor(ProPublica.url(endpoint));
	}

	protected static Bill fromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		Bill bill = new Bill();

        // Fail if this isn't present.
        bill.id = json.getString("bill_id");
        String[] pieces = Bill.splitBillId(bill.id);
        bill.bill_type = pieces[0];
        bill.number = Integer.parseInt(pieces[1]);
        bill.congress = Integer.parseInt(pieces[2]);

        // needs to happen after bill_type is calculated
        bill.chamber = Bill.chamberFrom(bill.bill_type);

        if (!json.isNull("title")) {
            bill.short_title = json.getString("title");
            bill.official_title = bill.short_title;
        }

        if (!json.isNull("latest_major_action_date"))
            bill.last_action_on = ProPublica.parseDateOnly(json.getString("latest_major_action_date"));

        if (!json.isNull("introduced_date"))
            bill.introduced_on = ProPublica.parseDateOnly(json.getString("introduced_date"));

        if (!json.isNull("house_passage"))
            bill.house_passage_result_on = ProPublica.parseDateOnly(json.getString("house_passage"));
        if (!json.isNull("senate_passage"))
            bill.senate_passage_result_on = ProPublica.parseDateOnly(json.getString("senate_passage"));

        if (!json.isNull("vetoed")) {
            bill.vetoed_on = ProPublica.parseDateOnly(json.getString("vetoed"));
            bill.vetoed = true;
        } else
            bill.vetoed = false;

        if (!json.isNull("enacted")) {
            bill.enacted_on = ProPublica.parseDateOnly(json.getString("enacted"));
            bill.enacted = true;
        } else
            bill.enacted = false;

        if (!json.isNull("active"))
            bill.active = json.getBoolean("active");
        else
            bill.active = false;

        if (!json.isNull("cosponsors"))
            bill.cosponsors_count = json.getInt("cosponsors");

        if (!json.isNull("summary"))
            bill.summary = json.getString("summary");

        if (!json.isNull("govtrack_url"))
            bill.govtrack_url = json.getString("govtrack_url");
        if (!json.isNull("congressdotgov_url"))
            bill.congress_url = json.getString("congressdotgov_url");
        if (!json.isNull("gpo_pdf_uri"))
            bill.gpo_url = json.getString("gpo_pdf_uri");

        if (!json.isNull("sponsor")) {
            Legislator sponsor = new Legislator();
            sponsor.chamber = bill.chamber;

            String[] names = Legislator.splitName(json.getString("sponsor"));
            sponsor.first_name = names[0];
            sponsor.last_name = names[1];

            sponsor.party = json.getString("sponsor_party");
            sponsor.state = json.getString("sponsor_state");
            sponsor.bioguide_id = json.getString("sponsor_id");

            bill.sponsor = sponsor;
        }

        if (!json.isNull("actions")) {
            JSONArray jsonActions = json.getJSONArray("actions");
            List<Bill.Action> actions = new ArrayList<Bill.Action>();

            for (int i=0; i<jsonActions.length(); i++) {
                JSONObject object = jsonActions.getJSONObject(i);
                Bill.Action action = new Bill.Action();
                action.id = object.getInt("id");
                action.acted_on = ProPublica.parseDateOnly(object.getString("datetime"));
                action.chamber = object.getString("chamber").toLowerCase();
                action.type = object.getString("action_type");
                action.description = object.getString("description");

                // Unique ID for indexing "seen" actions.
                action.full_id = bill.id + "-" + action.id;

                actions.add(action);
            }

            bill.actions = actions;

            if (bill.actions.size() > 0)
                bill.lastAction = bill.actions.get(0);
        }

        // if no actions array (list of bills), then make a minimal lastAction
        else if (!json.isNull("latest_major_action")) {
            bill.lastAction = new Bill.Action();
            bill.lastAction.description = json.getString("latest_major_action");
        }

        if (!json.isNull("votes")) {
            JSONArray jsonVotes = json.getJSONArray("votes");
            List<Bill.Vote> votes = new ArrayList<Bill.Vote>();
            for (int i=0; i<jsonVotes.length(); i++) {
                JSONObject object = jsonVotes.getJSONObject(i);
                Bill.Vote vote = new Bill.Vote();

                vote.voted_on = ProPublica.parseDateOnly(object.getString("date"));
                vote.chamber = object.getString("chamber").toLowerCase();
                vote.result = object.getString("result").toLowerCase();
                vote.question = object.getString("question");
                vote.yes = object.getInt("total_yes");
                vote.no = object.getInt("total_no");
                vote.not_voting = object.getInt("total_not_voting");

                int year = vote.voted_on.getYear() + 1900;
                String number = object.getString("roll_call");
                vote.roll_id = "" + vote.chamber.charAt(0) + number + "-" + year;

                // Unique ID for indexing seen votes.
                vote.full_id = bill.id + "-" + vote.roll_id;

                votes.add(vote);
            }

            bill.votes = votes;
        }

        return bill;
	}

	// TODO: remove once unused by RollService and UpcomingBillService
	protected static Bill fromSunlightAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		Bill bill = new Bill();

		if (!json.isNull("bill_id"))
			bill.id = json.getString("bill_id");
		
		if (!json.isNull("bill_type"))
			bill.bill_type = json.getString("bill_type");
		
		if (!json.isNull("chamber"))
			bill.chamber = json.getString("chamber");

		if (!json.isNull("congress"))
			bill.congress = json.getInt("congress");
		
		if (!json.isNull("number"))
			bill.number = json.getInt("number");
		
		if (!json.isNull("short_title"))
			bill.short_title = json.getString("short_title");
		if (!json.isNull("official_title"))
			bill.official_title = json.getString("official_title");
		if (!json.isNull("last_action_at"))
			bill.last_action_on = Congress.parseDateEither(json.getString("last_action_at"));

		if (!json.isNull("introduced_on"))
			bill.introduced_on = Congress.parseDateOnly(json.getString("introduced_on"));
		
		if (!json.isNull("cosponsors_count"))
			bill.cosponsors_count = json.getInt("cosponsors_count");
		
		// timeline dates
		if (!json.isNull("history")) {
			JSONObject history = json.getJSONObject("history");
			if (!history.isNull("house_passage_result_at"))
				bill.house_passage_result_on = Congress.parseDateEither(history.getString("house_passage_result_at"));
			if (!history.isNull("senate_passage_result_at"))
				bill.senate_passage_result_on = Congress.parseDateEither(history.getString("senate_passage_result_at"));
			if (!history.isNull("vetoed_at"))
				bill.vetoed_on = Congress.parseDateEither(history.getString("vetoed_at"));
            if (!history.isNull("enacted_at"))
				bill.enacted_on = Congress.parseDateEither(history.getString("enacted_at"));
	
			// timeline flags and values
			if (!history.isNull("active"))
				bill.active = history.getBoolean("active");
			if (!history.isNull("vetoed"))
				bill.vetoed = history.getBoolean("vetoed");
			if (!history.isNull("enacted"))
				bill.enacted = history.getBoolean("enacted");
		}

		if (!json.isNull("sponsor"))
			bill.sponsor = LegislatorService.fromSunlight(json.getJSONObject("sponsor"));

		if (!json.isNull("summary"))
			bill.summary = json.getString("summary");

		if (!json.isNull("cosponsors")) {
			JSONArray cosponsorObjects = json.getJSONArray("cosponsors");
			int length = cosponsorObjects.length();
			
			bill.cosponsors = new ArrayList<Legislator>();
			
			for (int i=0; i<length; i++)
				bill.cosponsors.add(LegislatorService.fromSunlight(cosponsorObjects.getJSONObject(i).getJSONObject("legislator")));
		}
		
		return bill;
	}

	/* Private helpers for loading single or plural bill objects */

	private static Bill billFor(String url) throws CongressException {
		try {
			JSONObject json = ProPublica.firstResult(url);
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
			JSONArray results = ProPublica.resultsFor(url);

            JSONArray jsonBills = results;
            if (jsonBills.length() > 0) {
                JSONObject firstResult = results.getJSONObject(0);
                if (!firstResult.isNull("bills"))
                    jsonBills = firstResult.getJSONArray("bills");
            }

			int length = jsonBills.length();
			for (int i = 0; i < length; i++)
				bills.add(fromAPI(jsonBills.getJSONObject(i)));

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
