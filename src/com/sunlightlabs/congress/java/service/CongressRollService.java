package com.sunlightlabs.congress.java.service;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Drumbone;
import com.sunlightlabs.congress.java.Roll;
import com.sunlightlabs.congress.java.Roll.Vote;

public class CongressRollService implements RollService {

	public Roll fromDrumbone(JSONObject json) throws CongressException {
		Roll roll = new Roll();
		try {
			if (!json.isNull("roll_id"))
				roll.id = json.getString("roll_id");
			if (!json.isNull("chamber"))
				roll.chamber = json.getString("chamber");
			if (!json.isNull("type"))
				roll.type = json.getString("type");
			if (!json.isNull("question"))
				roll.question = json.getString("question");
			if (!json.isNull("result"))
				roll.result = json.getString("result");
			if (!json.isNull("bill_id"))
				roll.bill_id = json.getString("bill_id");
			if (!json.isNull("required"))
				roll.required = json.getString("required");
			if (!json.isNull("number"))
				roll.number = json.getInt("number");
			if (!json.isNull("session"))
				roll.session = json.getInt("session");
			if (!json.isNull("year"))
				roll.year = json.getInt("year");
			if (!json.isNull("voted_at"))
				roll.voted_at = DateUtils
						.parseDate(json.getString("voted_at"), Drumbone.dateFormat);

			if (!json.isNull("bill"))
				roll.bill = Bill.service.fromDrumbone(json.getJSONObject("bill"));

			if (!json.isNull("vote_breakdown")) {
				JSONObject vote_breakdown = json.getJSONObject("vote_breakdown");
				Iterator<?> iter = vote_breakdown.keys();
				while (iter.hasNext()) {
					String key = (String) iter.next();
					if (key.equals("ayes"))
						roll.ayes = vote_breakdown.getInt(key);
					else if (key.equals("nays"))
						roll.nays = vote_breakdown.getInt(key);
					else if (key.equals("present"))
						roll.present = vote_breakdown.getInt(key);
					else if (key.equals("not_voting"))
						roll.not_voting = vote_breakdown.getInt(key);
					else
						roll.otherVotes.put(key, vote_breakdown.getInt(key));
				}
			}

			if (!json.isNull("voters")) {
				roll.voters = new HashMap<String, Vote>();
				JSONObject votersObject = json.getJSONObject("voters");
				Iterator<?> iter = votersObject.keys();
				while (iter.hasNext()) {
					String voter_id = (String) iter.next();
					roll.voters.put(voter_id, new Vote(voter_id, votersObject
							.getJSONObject(voter_id)));
				}
			}

			if (!json.isNull("voter_ids")) {
				roll.voter_ids = new HashMap<String, Vote>();
				JSONObject voterIdsObject = json.getJSONObject("voter_ids");
				Iterator<?> iter = voterIdsObject.keys();
				while (iter.hasNext()) {
					String voter_id = (String) iter.next();
					roll.voters.put(voter_id,
							new Vote(voter_id, voterIdsObject.getString(voter_id)));
				}
			}
		} catch (JSONException e) {

		} catch (DateParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return roll;
	}

	public Roll find(String id, String sections) throws CongressException {
		return rollFor(Drumbone.url("roll", "roll_id=" + id + "&sections=" + sections));
	}

	public Roll rollFor(String url) throws CongressException {
		String rawJSON = Drumbone.fetchJSON(url);
		try {
			return fromDrumbone(new JSONObject(rawJSON).getJSONObject("roll"));
		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
	}

}
