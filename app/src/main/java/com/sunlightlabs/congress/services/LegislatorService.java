package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegislatorService {

	private static String[] basicFields = new String[] {
		"bioguide_id", "thomas_id", "govtrack_id",
		"in_office", "party", "gender", "state", "state_name",
		"district", "title", "chamber", "senate_class", "birthday",
		"term_start", "term_end", "leadership_role",
		"first_name", "nickname", "middle_name", "last_name", "name_suffix",
		"phone", "website", "office",
		"twitter_id", "youtube_id", "facebook_id"
	};

	public static List<Legislator> allWhere(String key, String value) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put(key, value);
		params.put("order", "last_name__asc");
		params.put("per_page", "all");

		return legislatorsFor(Congress.url("legislators", basicFields, params));
	}

	public static List<Legislator> allForZipCode(String zip) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("zip", zip);
		return legislatorsFor(Congress.url("legislators/locate", basicFields, params));
	}

	public static List<Legislator> allForLatLong(double latitude, double longitude) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("latitude", String.valueOf(latitude));
		params.put("longitude", String.valueOf(longitude));
		return legislatorsFor(Congress.url("legislators/locate", basicFields, params));
	}

	public static List<Legislator> allForState(String state) throws CongressException {
        // /members/{chamber}/{state}/current.json
        String[] senate = new String[] { "members", "senate", state, "current" };
        String[] house = new String[] { "members", "house", state, "current" };

        List<Legislator> members = new ArrayList<Legislator>();

        List<Legislator> senators = proPublicaLegislatorsFor(ProPublica.url(senate));
        for (int i=0; i<senators.size(); i++) {
            senators.get(i).state = state;
            senators.get(i).chamber = "senate";
        }

        List<Legislator> representatives = proPublicaLegislatorsFor(ProPublica.url(house));
        for (int i=0; i<representatives.size(); i++) {
            representatives.get(i).state = state;
            representatives.get(i).chamber = "house";
        }

        members.addAll(senators);
        members.addAll(representatives);

        return members;
    }

	public static Legislator find(String bioguideId) throws CongressException {
        String[] paths = new String[] {"members", bioguideId};
		return proPublicaLegislatorFor(ProPublica.url(paths));
	}
	
	/* JSON parsers, also useful for other service endpoints within this package */

	protected static Legislator fromProPublica(JSONObject json) throws JSONException, CongressException {
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

        // API may return in_office as a string, but JSONObject should auto-coerce:
        // https://developer.android.com/reference/org/json/JSONObject.html
        if (!json.isNull("in_office"))
            legislator.in_office = json.getBoolean("in_office");

        if (!json.isNull("first_name"))
            legislator.first_name = json.getString("first_name");
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
        // TODO: a Role object on Legislator?
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

            if (!json.isNull("district"))
                legislator.district = json.getString("district");

            if (!json.isNull("name")) {
                String displayName = json.getString("name");
                String[] pieces = Legislator.splitName(displayName);
                legislator.first_name = pieces[0];
                legislator.last_name = pieces[1];
            }
        }

        return legislator;
    }

	protected static Legislator fromAPI(JSONObject json) throws JSONException, CongressException {
		if (json == null)
			return null;
		
		Legislator legislator = new Legislator();

		if (!json.isNull("bioguide_id"))
			legislator.bioguide_id = json.getString("bioguide_id");
		if (!json.isNull("govtrack_id"))
			legislator.govtrack_id = json.getString("govtrack_id");
		if (!json.isNull("thomas_id"))
			legislator.thomas_id = json.getString("thomas_id");
		
		if (!json.isNull("in_office"))
			legislator.in_office = json.getBoolean("in_office");

		if (!json.isNull("first_name"))
			legislator.first_name = json.getString("first_name");
		if (!json.isNull("last_name"))
			legislator.last_name = json.getString("last_name");
		if (!json.isNull("nickname"))
			legislator.nickname = json.getString("nickname");
		if (!json.isNull("name_suffix"))
			legislator.name_suffix = json.getString("name_suffix");
		
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
			return fromAPI(Congress.firstResult(url));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

	// TODO: rename to legislatorFor and remove old Sunlight method
	private static Legislator proPublicaLegislatorFor(String url) throws CongressException {
        try {
            return fromProPublica(ProPublica.firstResult(url));
        } catch (JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        }
    }

	private static List<Legislator> legislatorsFor(String url) throws CongressException {
		List<Legislator> legislators = new ArrayList<Legislator>();
		try {
			JSONArray results = Congress.resultsFor(url);

			int length = results.length();
			for (int i = 0; i < length; i++)
				legislators.add(fromAPI(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}

		return legislators;
	}

    // TODO: rename to legislatorsFor and remove old Sunlight method
    private static List<Legislator> proPublicaLegislatorsFor(String url) throws CongressException {
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
            }

            int length = members.length();
            for (int i = 0; i < length; i++)
                legislators.add(fromProPublica(members.getJSONObject(i)));

        } catch (JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        }

        return legislators;
    }

}