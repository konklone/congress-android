package com.sunlightlabs.congress.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.cookie.DateParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class CommitteeService {

    // /{congress}/{chamber}/committees/{committee_id}.json
	public static Committee find(String committee_id) throws CongressException {
        String chamber;
        if (committee_id.toLowerCase().startsWith("h"))
            chamber = "house";
        else if (committee_id.toLowerCase().startsWith("s"))
            chamber = "senate";
        else
            chamber = "joint";

        String congress = String.valueOf(Bill.currentCongress());

        String[] endpoint = { congress, chamber, "committees", committee_id };
		return committeeFor(ProPublica.url(endpoint));
	}

    // /{congress}/{chamber}/committees.json
	public static List<Committee> forChamber(String chamber) throws CongressException {
		String congress = String.valueOf(Bill.currentCongress());
        String[] endpoint = { congress, chamber, "committees" };
		return committeesFor(ProPublica.url(endpoint));
	}

	protected static Committee fromAPI(JSONObject json) throws JSONException, DateParseException, CongressException {
        Committee committee = new Committee();

        if (!json.isNull("id"))
            committee.id = json.getString("id");

        // is a subcommittee if it has a parent committee name/id
        if (!json.isNull("committee_name")) {
            committee.parent_committee_name = json.getString("committee_name");
            // TODO: parent committee ID (for now, do it in calling method)
            // committee.parent_committee_id = ___;
            committee.subcommittee = true;
        }

        if (!json.isNull("chamber"))
            committee.chamber = json.getString("chamber").toLowerCase();

        if (!json.isNull("name")) {
            committee.name = json.getString("name");

            // If we can/should, prefix committee name with chamber
            if (committee.chamber.equals("house"))
                committee.name = "House " + committee.name;
            else if (committee.chamber.equals("senate"))
                committee.name = "Senate " + committee.name;
        }

        if (!json.isNull("url"))
            committee.url = json.getString("url");

        if (!json.isNull("chair_id")) {
            Legislator chair = new Legislator();
            chair.bioguide_id = json.getString("chair_id");

            if (!json.isNull("chair")) {
                String[] names = Legislator.splitName(json.getString("chair"));
                chair.first_name = names[0];
                chair.last_name = names[1];
            }
            if (!json.isNull("chair_party"))
                chair.party = json.getString("chair_party");
            if (!json.isNull("chair_state"))
                chair.state = json.getString("chair_state");

            committee.chair = chair;
        }

        if (!json.isNull("subcommittees")) {
            JSONArray array = json.getJSONArray("subcommittees");
            List<Committee> subcommittees = new ArrayList<Committee>();

            for (int i=0; i<array.length(); i++) {
                Committee subcommittee = new Committee();
                subcommittee.parent_committee_id = committee.id;
                subcommittee.subcommittee = true;

                JSONObject object = array.getJSONObject(i);
                subcommittee.id = object.getString("id");
                subcommittee.name = object.getString("name");

                subcommittees.add(subcommittee);
            }

            committee.subcommittees = subcommittees;
        }

        // details endpoint only
        if (!json.isNull("current_members")) {
            String chair_id = json.getString("chair_id");
            String ranking_id = json.getString("ranking_member_id");

            List<Legislator> members = new ArrayList<Legislator>();
            JSONArray array = json.getJSONArray("current_members");

            for (int i=0; i<array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                Legislator member = new Legislator();

                if (!object.isNull("id"))
                    member.bioguide_id = object.getString("id");
                if (!object.isNull("party"))
                    member.party = object.getString("party");
                if (!object.isNull("state"))
                    member.state = object.getString("state");
                if (!object.isNull("name")) {
                    String[] names = Legislator.splitName(object.getString("name"));
                    member.first_name = names[0];
                    member.last_name = names[1];
                }

                member.membership = new Committee.Membership();
                // TODO: rank should really be absolute, not in party
                // TODO: side and title, hopefully
                if (!object.isNull("rank_in_party"))
                    member.membership.rank = Integer.valueOf(object.getString("rank_in_party"));
                else
                    member.membership.rank = Integer.MIN_VALUE; // issue #141
                member.membership.side = object.getString("side");

                // There's no title field, but if their bioguide matches, set them here
                if ((member.bioguide_id != null) && member.bioguide_id.equals(chair_id))
                    member.membership.title = "Chair";
                if ((member.bioguide_id != null) && member.bioguide_id.equals(ranking_id))
                    member.membership.title = "Ranking Member";

                members.add(member);
            }
            committee.members = members;
        }

        return committee;
    }
	
	private static Committee committeeFor(String url) throws CongressException {
        try {
            JSONArray results = ProPublica.resultsFor(url);
            return fromAPI(results.getJSONObject(0));
        } catch (JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        } catch (DateParseException e) {
            throw new CongressException(e, "Problem parsing date in JSON at " + url);
        }
    }

    private static List<Committee> committeesFor(String url) throws CongressException {
        List<Committee> committees = new ArrayList<Committee>();
        try {
            JSONArray results = ProPublica.resultsFor(url);

            if (results.length() > 0) {
                JSONObject firstResult = results.getJSONObject(0);
                if (!firstResult.isNull("committees"))
                    results = firstResult.getJSONArray("committees");
            }

            int length = results.length();
            for (int i = 0; i < length; i++)
                committees.add(fromAPI(results.getJSONObject(i)));

        } catch (JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        } catch (DateParseException e) {
            throw new CongressException(e, "Problem parsing date in JSON at " + url);
        }

        return committees;
    }

}