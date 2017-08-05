package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.models.Roll.Vote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RollService {

    public static String datetimeFormat = "yyyy-MM-dd hh:mm:ss";

    // standard field names that map to Roll vote type names
    public static final String YEA_FIELD = "yes";
    public static final String NAY_FIELD = "no";
    public static final String PRESENT_FIELD = "present";
    public static final String NOT_VOTING_FIELD = "not_voting";

    // standard response in Pro Publica API for vote positions
    public static final String YEA = "Yes";
    public static final String NAY = "No";
    public static final String PRESENT = "Present";
    public static final String NOT_VOTING = "Not Voting";

    // Pro Publica API uses "Speaker" to represent Speaker not voting
    public static final String SPEAKER = "Speaker";

    // Pro Publica API uses these to denote certain House vote types
    public static final String VOTE_HALF_ONE = "YEA-AND-NAY";
    public static final String VOTE_HALF_TWO = "RECORDED VOTE";
    public static final String VOTE_TWO_THIRDS  = "2/3 YEA-AND-NAY";



    public static String[] basicFields = {
		"roll_id", "chamber", "number", "year", "congress", "bill_id",
		"bill.official_title", "bill.short_title",
		"voted_at", "vote_type", "roll_type", "question", "required", "result",
		"breakdown"
	};

    public static List<Roll> latestVotes(int page, int per_page) throws CongressException {
        Map<String,String> params = new HashMap<String,String>();
        params.put("order", "voted_at");
        return sunlightRollsFor(Congress.url("votes", basicFields, params, page, per_page));
    }

    // /{congress}/{chamber}/sessions/{session-number}/votes/{roll-call-number}.json
	public static Roll find(String id) throws CongressException {
		Roll bare = Roll.splitRollId(id);
        String chamber = bare.chamber;
        String number = String.valueOf(bare.number);

        int year = bare.year;
        String congress = String.valueOf(Bill.congressForYear(year));
        String session = String.valueOf(Bill.sessionForYear(year));

        String[] endpoint = { congress, chamber, "sessions", session, "votes", number };
		return rollFor(ProPublica.url(endpoint));
	}
	
	public static List<Roll> latestMemberVotes(String bioguideId, String chamber, int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("order", "voted_at");
		params.put("chamber", chamber);
		params.put("voter_ids." + bioguideId + "__exists", "true");
		
		String[] fields = new String[basicFields.length + 1];
		System.arraycopy(basicFields, 0, fields, 0, basicFields.length);
		fields[basicFields.length + 0] = "voter_ids." + bioguideId;

		return sunlightRollsFor(Congress.url("votes", fields, params, page, per_page));
	}

	protected static Roll fromAPI(JSONObject json) throws JSONException, ParseException {
        Roll roll = new Roll();

        if (!json.isNull("congress"))
            roll.congress = Integer.valueOf(json.getString("congress"));
        if (!json.isNull("chamber"))
            roll.chamber = json.getString("chamber").toLowerCase();
        if (!json.isNull("roll_call"))
            roll.number = Integer.valueOf(json.getString("roll_call"));
        if (!json.isNull("session"))
            roll.session = Integer.valueOf(json.getString("session"));

        roll.id = Roll.makeRollId(roll.chamber, roll.number, roll.session);

        if (!json.isNull("question"))
            roll.question = json.getString("question");
        if (!json.isNull("result"))
            roll.result = json.getString("result");
        if (!json.isNull("description"))
            roll.description = json.getString("description");

        // In the Senate, the vote_type is just the fraction (1/2, 3/5)
        // In the House, it can be one of a few denotations.
        if (!json.isNull("vote_type")) {
            String vote_type = json.getString("vote_type");
            if (roll.chamber.equals("senate"))
                roll.required = vote_type;
            else {
                if (vote_type.equals(RollService.VOTE_HALF_ONE))
                    roll.required = "1/2";
                else if (vote_type.equals(RollService.VOTE_HALF_TWO))
                    roll.required = "1/2";
                else if (vote_type.equals(RollService.VOTE_TWO_THIRDS))
                    roll.required = "2/3";
                else // can be QUORUM, pass it through
                    roll.required = vote_type;
            }
        }

        // date and time fields make up a timestamp in Congress' time
        if (!json.isNull("date") && !json.isNull("time")) {
            String timestamp = json.getString("date") + " " + json.getString("time");
            SimpleDateFormat format = new SimpleDateFormat(datetimeFormat);
            format.setTimeZone(ProPublica.CONGRESS_TIMEZONE);
            roll.voted_at = format.parse(timestamp);
        }

        // for now, not making use of latest_action
        if (!json.isNull("bill")) {
            JSONObject bill = json.getJSONObject("bill");
            roll.bill_id = bill.getString("bill_id");
            roll.bill_title = bill.getString("title");
        }

        roll.voteBreakdown.put(Roll.YEA, 0);
        roll.voteBreakdown.put(Roll.NAY, 0);
        roll.voteBreakdown.put(Roll.PRESENT, 0);
        roll.voteBreakdown.put(Roll.NOT_VOTING, 0);

        if (!json.isNull("total")) {
            JSONObject total = json.getJSONObject("total");

            // map yes/no/present/not_voting to Yea/Nay/Present/Not Voting
            // but let Speaker votes go right through,
            // and mark the roll call as having non-standard votes
            Iterator<?> iter = total.keys();
            while (iter.hasNext()) {
                String key = (String) iter.next();

                if (key.equals(RollService.YEA_FIELD))
                    roll.voteBreakdown.put(Roll.YEA, total.getInt(key));
                else if (key.equals(RollService.NAY_FIELD))
                    roll.voteBreakdown.put(Roll.NAY, total.getInt(key));
                else if (key.equals(RollService.NOT_VOTING_FIELD))
                    roll.voteBreakdown.put(Roll.NOT_VOTING, total.getInt(key));
                else if (key.equals(RollService.PRESENT_FIELD))
                    roll.voteBreakdown.put(Roll.PRESENT, total.getInt(key));
                else {
                    roll.voteBreakdown.put(key, total.getInt(key));
                    roll.otherVotes = true;
                }
            }

            // if we detected there was a Speaker vote, remove yes/no from the breakdown
            if (roll.otherVotes) {
                roll.voteBreakdown.remove(Roll.YEA);
                roll.voteBreakdown.remove(Roll.NAY);
            }
        }

        // if there was a tiebreaker vote
        // API bug: these can be empty strings instead of null
        if (!json.isNull("tie_breaker") && !json.getString("tie_breaker").equals(""))
            roll.tie_breaker = json.getString("tie_breaker");
        if (!json.isNull("tie_breaker_vote") && !json.getString("tie_breaker_vote").equals(""))
            roll.tie_breaker_vote = json.getString("tie_breaker_vote");

        if (!json.isNull("positions")) {
            roll.voters = new HashMap<String, Vote>();
            JSONArray positions = json.getJSONArray("positions");
            for (int i=0; i<positions.length(); i++) {
                JSONObject position = positions.getJSONObject(i);
                String voter_id = position.getString("member_id");
                String vote_position = position.getString("vote_position");

                // skip any non-votes *by* the Speaker
                // this is how Pro Publica API represents Speaker votes
                if (vote_position.equals(RollService.SPEAKER))
                    continue;

                Roll.Vote vote = new Roll.Vote();
                vote.voter_id = voter_id;
                if (vote_position.equals(RollService.YEA))
                    vote.vote = Roll.YEA;
                else if (vote_position.equals(RollService.NAY))
                    vote.vote = Roll.NAY;
                else if (vote_position.equals(RollService.NOT_VOTING))
                    vote.vote = Roll.NOT_VOTING;
                else if (vote_position.equals(RollService.PRESENT))
                    vote.vote = Roll.PRESENT;
                else
                    vote.vote = vote_position;

                roll.voters.put(voter_id, vote);
            }
        }

        return roll;
    }

	protected static Roll fromSunlightAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		if (json == null)
			throw new CongressException("Error loading votes.");
		
		Roll roll = new Roll();
		
		if (!json.isNull("chamber"))
			roll.chamber = json.getString("chamber");
		if (!json.isNull("question"))
			roll.question = json.getString("question");
		if (!json.isNull("result"))
			roll.result = json.getString("result");
		if (!json.isNull("congress"))
			roll.congress = json.getInt("congress");
		if (!json.isNull("year"))
			roll.year = json.getInt("year");
		if (!json.isNull("voted_at"))
			roll.voted_at = Congress.parseDate(json.getString("voted_at"));
		
		// guaranteed fields for roll call votes
		if (!json.isNull("required"))
			roll.required = json.getString("required");
		if (!json.isNull("number"))
			roll.number = json.getInt("number");
		if (!json.isNull("roll_id"))
			roll.id = json.getString("roll_id");
		
		
		if (!json.isNull("bill_id"))
			roll.bill_id = json.getString("bill_id");

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
				
				Roll.Vote vote = voteFromSunlightAPI(voter_id, voterObject);
				
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
				
				roll.voter_ids.put(voter_id, voteFromSunlightAPI(voter_id, vote_name));
			}
		}

		return roll;
	}

	protected static Vote voteFromSunlightAPI(String voter_id, JSONObject json) throws JSONException, CongressException {
		Vote vote = new Vote();
		vote.vote = json.getString("vote");
		vote.voter_id = voter_id;
		vote.voter = LegislatorService.fromSunlight(json.getJSONObject("voter"));
		if (vote.voter == null)
			return null;
		else
			return vote;
	}
	
	protected static Vote voteFromSunlightAPI(String voter_id, String vote_name) throws JSONException, CongressException {
		Vote vote = new Vote();
		vote.vote = vote_name;
		vote.voter_id = voter_id;
		return vote;
	}

    // TODO: delete after migration
	private static List<Roll> sunlightRollsFor(String url) throws CongressException {
		List<Roll> rolls = new ArrayList<Roll>();
		try {
			JSONArray results = Congress.resultsFor(url);

			int length = results.length();
			for (int i = 0; i < length; i++)
				rolls.add(fromSunlightAPI(results.getJSONObject(i)));
			
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return rolls;
	}

	// needs to do its own fetching, as the PP API uses an inconsistent
    // type for the 'results' field on the vote-fetching endpoint
    private static Roll rollFor(String url) throws CongressException {
        try {
            String rawJSON = ProPublica.fetchJSON(url);
            JSONObject response = new JSONObject(rawJSON);

            // First check that the Pro Publica API said 'OK'
            String status = response.getString("status");
            if (!status.equals("OK"))
                throw new CongressException("Got a non-OK status from " + url + "\n\n" + rawJSON);

            JSONObject voteObject = response.getJSONObject("results")
                    .getJSONObject("votes")
                    .getJSONObject("vote");

            return fromAPI(voteObject);
        } catch (JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        } catch (ParseException e) {
            throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
        }
    }

    // Custom parsing method for votes by date
	private static List<Roll> rollsFor(String url) throws CongressException {
		List<Roll> rolls = new ArrayList<Roll>();
        try {
            String rawJSON = ProPublica.fetchJSON(url);
            JSONObject response = new JSONObject(rawJSON);

            // First check that the Pro Publica API said 'OK'
            String status = response.getString("status");
            if (!status.equals("OK"))
                throw new CongressException("Got a non-OK status from " + url + "\n\n" + rawJSON);

            JSONArray voteObjects = response.getJSONObject("results")
                    .getJSONArray("votes");

            for (int i=0; i<voteObjects.length(); i++) {
                JSONObject vote = voteObjects.getJSONObject(i);
                Roll roll = fromAPI(vote);
                rolls.add(roll);
            }

        } catch (JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        } catch (ParseException e) {
            throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
        }

		return rolls;
	}

}