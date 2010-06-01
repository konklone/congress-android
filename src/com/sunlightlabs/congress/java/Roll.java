package com.sunlightlabs.congress.java;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class Roll {
	public static final int OTHER = -1;
	public static final int AYE = 0;
	public static final int NAY = 1;
	public static final int NOT_VOTING = 2;
	public static final int PRESENT = 3;
	
	// basic
	public String id, chamber, type, question, result, bill_id, required; 
	public int session, number, year;
	public Date voted_at;
	public int ayes, nays, present, not_voting;
	public HashMap<String,Integer> otherVotes = new HashMap<String,Integer>();
	
	// bill
	public Bill bill;
	
	// voters
	public HashMap<String,Vote> voters;
	
	// voter_ids
	public HashMap<String,Vote> voter_ids;
	
	public Roll() {}
	
	public Roll(JSONObject json) throws JSONException, DateParseException {
		if (!json.isNull("roll_id"))
			id = json.getString("roll_id");
		if (!json.isNull("chamber"))
			chamber = json.getString("chamber");
		if (!json.isNull("type"))
			type = json.getString("type");
		if (!json.isNull("question"))
			question = json.getString("question");
		if (!json.isNull("result"))
			result = json.getString("result");
		if (!json.isNull("bill_id"))
			bill_id = json.getString("bill_id");
		if (!json.isNull("required"))
			required = json.getString("required");
		if (!json.isNull("number"))
			number = json.getInt("number");
		if (!json.isNull("session"))
			session = json.getInt("session");
		if (!json.isNull("year"))
			year = json.getInt("year");
		if (!json.isNull("voted_at"))
			voted_at =  DateUtils.parseDate(json.getString("voted_at"), Drumbone.dateFormat);
		
		if (!json.isNull("bill"))
			bill = new Bill(json.getJSONObject("bill"));
		
		if (!json.isNull("vote_breakdown")) {
			JSONObject vote_breakdown = json.getJSONObject("vote_breakdown");
			Iterator<?> iter = vote_breakdown.keys();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				if (key.equals("ayes"))
					ayes = vote_breakdown.getInt(key);
				else if (key.equals("nays"))
					nays = vote_breakdown.getInt(key);
				else if (key.equals("present"))
					present = vote_breakdown.getInt(key);
				else if (key.equals("not_voting"))
					not_voting = vote_breakdown.getInt(key);
				else
					otherVotes.put(key, vote_breakdown.getInt(key));
			}
		}
		
		if (!json.isNull("voters")) {
			voters = new HashMap<String,Vote>();
			JSONObject votersObject = json.getJSONObject("voters");
			Iterator<?> iter = votersObject.keys();
			while (iter.hasNext()) {
				String voter_id = (String) iter.next();
				voters.put(voter_id, new Vote(voter_id, votersObject.getJSONObject(voter_id)));
			}
		}
		
		if (!json.isNull("voter_ids")) {
			voter_ids = new HashMap<String,Vote>();
			JSONObject voterIdsObject = json.getJSONObject("voter_ids");
			Iterator<?> iter = voterIdsObject.keys();
			while (iter.hasNext()) {
				String voter_id = (String) iter.next();
				voters.put(voter_id, new Vote(voter_id, voterIdsObject.getString(voter_id)));
			}
		}
	}
	
	/**
	 * Represents the vote of a legislator in a roll call. In almost all cases, votes will be 
	 * AYE, NAY, PRESENT, or NOT_VOTING.  In these cases, the 'vote' field will be set to the 
	 * appropriate constant, and voteName will contain the text representation.
	 * 
	 * In one case, the election of the Speaker of the House, votes are recorded as the last name
	 * of the candidate. In this case, the 'vote' integer field will be set to OTHER, and the String field
	 * voteName will contain the text of the name of their vote.
	 * 
	 * The 'legislator' field may be null here, in which case you will need to use the bioguide_id
	 * to look up more information about the legislator.
	 */
	public class Vote implements Comparable<Vote> {
		public String voter_id; // bioguide ID
		
		public String vote_name;
		public int vote;
		
		public Legislator voter;
		
		public Vote(String voter_id, JSONObject json) throws JSONException, DateParseException {
			this.voter_id = voter_id;
			this.vote_name = json.getString("vote");
			this.vote = Roll.voteForName(this.vote_name);
			this.voter = Legislator.fromDrumbone(json.getJSONObject("voter"));
		}
		
		public Vote(String voter_id, String vote_name) {
			this.voter_id = voter_id;
			this.vote_name = vote_name;
			this.vote = Roll.voteForName(vote_name);
		}
		
		public int compareTo(Vote another) {
			return this.voter.compareTo(another.voter);
		}
	}
	
	public static int voteForName(String name) {
		if (name.equals("+"))
			return Roll.AYE;
		else if (name.equals("-"))
			return Roll.NAY;
		else if (name.equals("P"))
			return Roll.PRESENT;
		else if (name.equals("0"))
			return Roll.NOT_VOTING;
		else
			return Roll.OTHER;
	}
	
	// splits a roll into chamber, number, and year, returned in a barebones Roll object
	public static Roll splitRollId(String roll_id) {
		Pattern pattern = Pattern.compile("^([a-z]+)(\\d+)-(\\d{4})$");
		Matcher matcher = pattern.matcher(roll_id);
		if (!matcher.matches())
			return null;
		Roll roll = new Roll();
		
		String chamber = matcher.group(1);
		if (chamber.equals("h"))
			roll.chamber = "house";
		else // if (chamber.equals("s")
			roll.chamber = "senate";
		roll.number = Integer.parseInt(matcher.group(2));
		roll.year = Integer.parseInt(matcher.group(3));
		
		return roll;
	}
	
	
	/*
	 * Network methods - will be decoupled at some point.
	 */
	
	public static Roll find(String id, String sections) throws CongressException {
		return rollFor(Drumbone.url("roll", "roll_id=" + id + "&sections=" + sections));
	}
	
	public static Roll rollFor(String url) throws CongressException {
		String rawJSON = Drumbone.fetchJSON(url);
		try {
			return new Roll(new JSONObject(rawJSON).getJSONObject("roll"));
		} catch(JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch(DateParseException e) {
			throw new CongressException(e, "Problem parsing a date from the JSON from " + url);
		}
	}
}