package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CommitteeService {

    public static Committee find(String committee_id) throws CongressException {
        return find(committee_id, null);
    }

    // /{congress}/{chamber}/committees/{committee_id}.json
    // /{congress}/{chamber}/committees/{committee_id}/subcommittees/{subcommittee_id}.json
	public static Committee find(String committee_id, String subcommittee_id) throws CongressException {
        String chamber;
        if (committee_id.toLowerCase().startsWith("h"))
            chamber = "house";
        else if (committee_id.toLowerCase().startsWith("s"))
            chamber = "senate";
        else
            chamber = "joint";

        String congress = String.valueOf(Bill.currentCongress());

        // slightly awkward conditional because of limits on String[] initializer syntax
        if (subcommittee_id == null) {
            String[] endpoint = {congress, chamber, "committees", committee_id};
            return committeeFor(ProPublica.url(endpoint));
        } else {
            String[] endpoint = {congress, chamber, "committees", committee_id, "subcommittees", subcommittee_id};
            return committeeFor(ProPublica.url(endpoint));
        }
	}

    // /{congress}/{chamber}/committees.json
	public static List<Committee> forChamber(String chamber) throws CongressException {
		String congress = String.valueOf(Bill.currentCongress());
        String[] endpoint = { congress, chamber, "committees" };
		return committeesFor(ProPublica.url(endpoint));
	}

	protected static Committee fromAPI(JSONObject json) throws JSONException {
        Committee committee = new Committee();

        if (!json.isNull("id"))
            committee.id = json.getString("id");

        // is a subcommittee if it has a parent committee name/id
        if (!json.isNull("committee_name")) {
            committee.parent_committee_name = json.getString("committee_name");
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
			List<Committee> subcommittees = new ArrayList<>();

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

			List<Legislator> members = new ArrayList<>();
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

                // if a chamber field exists for members, assign it (needed for joint committees)
                if (!object.isNull("chamber"))
                    member.chamber = object.getString("chamber").toLowerCase();
                // if it's missing and a House or Senate committee, assign the members' chamber
                else if ((committee.chamber != null) && (committee.chamber.equals("house") || committee.chamber.equals("senate")))
                    member.chamber = committee.chamber;

                member.membership = new Committee.Membership();
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

    // parses committee and subcommittee info from fields on legislator roles
    protected static List<Committee> committeesFromArray(JSONArray list, boolean subcommittee) throws JSONException {
		List<Committee> committees = new ArrayList<>();
        for (int i=0; i<list.length(); i++) {
            JSONObject object = list.getJSONObject(i);
            Committee committee = new Committee();
            committee.name = object.getString("name");
            committee.id = object.getString("code");

            committee.subcommittee = subcommittee;

            if (committee.subcommittee) {
                if (!object.isNull("parent_committee_id"))
                    committee.parent_committee_id = object.getString("parent_committee_id");
                // fallback
                else
                    committee.parent_committee_id = committee.id.substring(0, 4);
            }

            if (!object.isNull("chamber"))
                committee.chamber = object.getString("chamber").toLowerCase();
            // fallback in case this field disappears (it was added by request)
            else if (committee.id.startsWith("H"))
                committee.chamber = "house";
            else if (committee.id.startsWith("S"))
                committee.chamber = "senate";
            else // if (committee.id.startsWith("J"))
                committee.chamber = "joint";

            committees.add(committee);
        }

        return committees;
    }
	
	private static Committee committeeFor(String url) throws CongressException {
        try {
            JSONArray results = ProPublica.resultsFor(url);
            return fromAPI(results.getJSONObject(0));
        } catch (JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        }
    }

    private static List<Committee> committeesFor(String url) throws CongressException {
		List<Committee> committees = new ArrayList<>();
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
        }

        return committees;
    }

}