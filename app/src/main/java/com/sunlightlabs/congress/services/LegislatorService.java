package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

// See Pro Publica Congress API docs:
// https://projects.propublica.org/api-docs/congress-api/endpoints/

public class LegislatorService {

	public static List<Legislator> allForState(String state) throws CongressException {
        // /members/{chamber}/{state}/current.json
        String[] senate = new String[] { "members", "senate", state, "current" };
        String[] house = new String[] { "members", "house", state, "current" };

        List<Legislator> members = new ArrayList<Legislator>();

        List<Legislator> senators = legislatorsFor(ProPublica.url(senate));
        for (int i=0; i<senators.size(); i++) {
            senators.get(i).state = state;
            senators.get(i).chamber = "senate";
        }

        List<Legislator> representatives = legislatorsFor(ProPublica.url(house));
        for (int i=0; i<representatives.size(); i++) {
            representatives.get(i).state = state;
            representatives.get(i).chamber = "house";
        }

        members.addAll(senators);
        members.addAll(representatives);

        return members;
    }

    // Expensive: request data for both chambers, and search client-side.
    public static List<Legislator> allByLastName(String name) throws CongressException {
        List<Legislator> members = new ArrayList<Legislator>();
        members.addAll(allByChamber("senate"));
        members.addAll(allByChamber("house"));

        String lower = name.toLowerCase();

        List<Legislator> matches = new ArrayList<Legislator>();

        // client-side match, nice
        for (int i=0; i<members.size(); i++) {
            Legislator member = members.get(i);
            if (member.last_name != null && member.last_name.toLowerCase().contains(lower))
                matches.add(member);
            else if (member.first_name != null && member.first_name.toLowerCase().contains(lower))
                matches.add(member);
            else if (member.middle_name != null && member.middle_name.toLowerCase().contains(lower))
                matches.add(member);
        }

        return matches;
    }

    // /{congress}/bills/{bill_number}{bill_type}/cosponsors.json
    public static List<Legislator> allCosponsors(String bill_id) throws CongressException {
        String congress = String.valueOf(Bill.currentCongress());
        String[] pieces = Bill.splitBillId(bill_id);
        String billNumber = pieces[0] + pieces[1];
        String[] endpoint = { congress, "bills", billNumber, "cosponsors" };
        List<Legislator> members = legislatorsFor(ProPublica.url(endpoint));

        // Fill in chamber field based on bill_id
        String chamber = Bill.chamberFrom(pieces[0]);
        for (int i=0; i<members.size(); i++)
            members.get(i).chamber = chamber;

        return members;
    }


    public static List<Legislator> allByChamber(String chamber) throws CongressException {
        // /{congress}/{chamber}/members.json
        String[] endpoint = { String.valueOf(Bill.currentCongress()), chamber, "members" };
        List<Legislator> members = legislatorsFor(ProPublica.url(endpoint));

        // The 'chamber' field is omitted in responses
        for (int i=0; i<members.size(); i++)
            members.get(i).chamber = chamber;

        return members;
    }

	public static Legislator find(String bioguideId) throws CongressException {
        String[] endpoint = new String[] {"members", bioguideId};
		return legislatorFor(ProPublica.url(endpoint));
	}
	
	/* JSON parsers, also useful for other service endpoints within this package */

	protected static Legislator fromAPI(JSONObject json) throws ParseException, JSONException, CongressException {
        if (json == null)
            return null;

        Legislator legislator = new Legislator();

        if (!json.isNull("member_id"))
            legislator.bioguide_id = json.getString("member_id");
        // on some endpoints, the bioguide ID is in the 'id' field
        else if (!json.isNull("id"))
            legislator.bioguide_id = json.getString("id");

        if (!json.isNull("govtrack_id"))
            legislator.govtrack_id = json.getString("govtrack_id");

        if (!json.isNull("in_office"))
            legislator.in_office = json.getBoolean("in_office");

        if (!json.isNull("at_large"))
            legislator.at_large = json.getBoolean("at_large");

        if (!json.isNull("first_name"))
            legislator.first_name = json.getString("first_name");
        if (!json.isNull("middle_name"))
            legislator.middle_name = json.getString("middle_name");
        if (!json.isNull("last_name"))
            legislator.last_name = json.getString("last_name");

        if (!json.isNull("gender"))
            legislator.gender = json.getString("gender");

        if (!json.isNull("url"))
            legislator.website = json.getString("url");

        if (!json.isNull("youtube_account")) {
            String youtube = json.getString("youtube_account");
            if (!youtube.isEmpty()) legislator.youtube_id = youtube;
        }
        if (!json.isNull("twitter_account")) {
            String twitter = json.getString("twitter_account");
            if (!twitter.isEmpty()) legislator.twitter_id = twitter;
        }
        if (!json.isNull("facebook_account")) {
            String facebook = json.getString("facebook_account");
            if (!facebook.isEmpty()) legislator.facebook_id = facebook;
        }

        // Some fields come from the legislator's current role.
        // If we have a roles array, use it for some data.
        // Otherwise, see if we can scrounge up data on the main object.
        if (!json.isNull("roles")) {
            JSONArray roles = json.getJSONArray("roles");
            if (roles.length() == 0) return null;
            JSONObject role = (JSONObject) roles.get(0);
            if (role == null) return null;

            // PP API uses long title (e.g. "Representative", "Senator, 3rd Class")
            // We currently store/display short titles (e.g. "Rep", "Sen").
            if (!role.isNull("title")) {
                String longTitle = role.getString("title");
                legislator.title = Legislator.shortTitle(longTitle);
            }

            if (!role.isNull("at_large"))
                legislator.at_large = role.getBoolean("at_large");

            if (!role.isNull("party"))
                legislator.party = role.getString("party");
            if (!role.isNull("state"))
                legislator.state = role.getString("state");
            if (!role.isNull("district"))
                legislator.district = role.getString("district");
            if (!role.isNull("chamber"))
                legislator.chamber = role.getString("chamber").toLowerCase();
            if (!role.isNull("start_date"))
                legislator.term_start = role.getString("start_date");
            if (!role.isNull("end_date"))
                legislator.term_end = role.getString("end_date");
            if (!role.isNull("leadership_role"))
                legislator.leadership_role = role.getString("leadership_role");
            if (!role.isNull("office"))
                legislator.office = role.getString("office");
            if (!role.isNull("phone"))
                legislator.phone = role.getString("phone");
        }

        // most minimal form: list of cosponsors, ID and name only
        else if (!json.isNull("cosponsor_id")) {
            legislator.bioguide_id = json.getString("cosponsor_id");

            if (!json.isNull("name")) {
                String[] names = Legislator.splitName(json.getString("name"));
                legislator.first_name = names[0];
                legislator.last_name = names[1];
                legislator.cosponsored_on = ProPublica.parseDateOnly(json.getString("date"));
            }

            if (!json.isNull("cosponsor_party"))
                legislator.party = json.getString("cosponsor_party");

            if (!json.isNull("cosponsor_state"))
                legislator.state = json.getString("cosponsor_state");

            if (!json.isNull("cosponsor_title"))
                legislator.title = Legislator.trimTitle(json.getString("cosponsor_title"));
        }

        // There's no 'roles' object, so we're parsing from a list endpoint.
        else {
            // Currently, this is parsing only from the state/district list.

            // On some endpoints, the field can be called 'role'.
            // https://github.com/propublica/congress-api-docs/issues/43
            if (!json.isNull("role")) {
                String longTitle = json.getString("role");
                legislator.title = Legislator.shortTitle(longTitle);
            }

            if (!json.isNull("party"))
                legislator.party = json.getString("party");

            if (!json.isNull("state"))
                legislator.state = json.getString("state");

            if (!json.isNull("district"))
                legislator.district = json.getString("district");

            // assume that if first_name is there, all 3 are there
            if (!json.isNull("first_name")) {
                legislator.first_name = json.getString("first_name");
                legislator.middle_name = json.getString("middle_name");
                legislator.last_name = json.getString("last_name");
            }
            else if (!json.isNull("name")) {
                String displayName = json.getString("name");
                String[] pieces = Legislator.splitName(displayName);
                legislator.first_name = pieces[0];
                legislator.last_name = pieces[1];
            }
        }

        return legislator;
    }

    // TODO: on the chopping block once unused from other Services
	protected static Legislator fromSunlight(JSONObject json) throws JSONException, CongressException {
		if (json == null)
			return null;
		
		Legislator legislator = new Legislator();

		if (!json.isNull("bioguide_id"))
			legislator.bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			legislator.govtrack_id = json.getString("govtrack_id");

		if (!json.isNull("in_office"))
			legislator.in_office = json.getBoolean("in_office");

		if (!json.isNull("first_name"))
			legislator.first_name = json.getString("first_name");
		if (!json.isNull("last_name"))
			legislator.last_name = json.getString("last_name");

		if (!json.isNull("title"))
			legislator.title = json.getString("title");
		if (!json.isNull("party"))
			legislator.party = json.getString("party");
		if (!json.isNull("state"))
			legislator.state = json.getString("state");
		if (!json.isNull("district"))
			legislator.district = json.getString("district");
		if (!json.isNull("chamber"))
			legislator.chamber = json.getString("chamber");
		if (!json.isNull("term_start"))
			legislator.term_start = json.getString("term_start");
		if (!json.isNull("term_end"))
			legislator.term_end = json.getString("term_end");
        if (!json.isNull("leadership_role"))
            legislator.leadership_role = json.getString("leadership_role");

		if (!json.isNull("gender"))
			legislator.gender = json.getString("gender");
		if (!json.isNull("office"))
			legislator.office = json.getString("office");
		if (!json.isNull("website"))
			legislator.website = json.getString("website");
		if (!json.isNull("phone"))
			legislator.phone = json.getString("phone");
		if (!json.isNull("youtube_id"))
			legislator.youtube_id = json.getString("youtube_id");
		if (!json.isNull("twitter_id"))
			legislator.twitter_id = json.getString("twitter_id");
		if (!json.isNull("facebook_id"))
			legislator.facebook_id = json.getString("facebook_id");
		
		return legislator;
	}

	private static Legislator legislatorFor(String url) throws CongressException {
        try {
            return fromAPI(ProPublica.firstResult(url));
        } catch (JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        } catch (ParseException e) {
            throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
        }
    }

    private static List<Legislator> legislatorsFor(String url) throws CongressException {
        List<Legislator> legislators = new ArrayList<Legislator>();
        try {
            JSONArray results = ProPublica.resultsFor(url);

            // 'get specific member' puts members in 'results'.
            // 'get current members by state/district' also uses 'results'.
            // But other member responses use a subfield of "members".
            // Need to introspect on the first result object to figure out
            // if that's the case. Documented behavior here:
            // https://github.com/propublica/congress-api-docs/issues/42

            JSONArray members = results;
            if (results.length() > 0) {
                JSONObject firstResult = results.getJSONObject(0);
                if (!firstResult.isNull("members"))
                    members = firstResult.getJSONArray("members");
                else if (!firstResult.isNull("cosponsors"))
                    members = firstResult.getJSONArray("cosponsors");
            }

            int length = members.length();
            for (int i = 0; i < length; i++)
                legislators.add(fromAPI(members.getJSONObject(i)));

        } catch (JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        } catch (ParseException e) {
            throw new CongressException(e, "Problem parsing date in the JSON from " + url);
        }

        return legislators;
    }

}