package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Roll implements Serializable {
	private static final long serialVersionUID = 1L;

    // standard names for types of votes
	public static final String YEA = "Yea";
	public static final String NAY = "Nay";
	public static final String NOT_VOTING = "Not Voting";
	public static final String PRESENT = "Present";

	// convenience flag, trip if there are non-standard votes (Speaker of the House election)
	public boolean otherVotes = false;
	
	// basic
	public int congress, number, year, session;
	public String id, chamber;
    public String question, result, description, required;
    public Date voted_at;

	public Map<String, Integer> voteBreakdown = new HashMap<>();

    // if there was a tie breaker
    public String tie_breaker, tie_breaker_vote;

    // if a bill is associated
    public String bill_id, bill_title;

	public Map<String,Vote> voters;

    // convenience field for when this is loaded for a specific member
    public String member_position;
	
	/**
	 * Represents the vote of a legislator in a roll call. In almost all cases, votes will be 
	 * "Yea", "Nay", "Present", or "Not Voting". There are constants for these as well, since 
	 * they have official meanings.
	 * 
	 * In one case, the election of the Speaker of the House, votes are recorded as the last name
	 * of the candidate.
	 * 
	 * The 'legislator' field may be null here, in which case you will need to use the bioguide_id
	 * to look up more information about the legislator.
	 */
	public static class Vote implements Comparable<Vote>, Serializable {
		private static final long serialVersionUID = 1L;
		
		public String voter_id; // bioguide ID
		public String vote;
		
		public Legislator voter;
		
		public Vote() {}

        @Override
        public int compareTo(Vote another) {
            return this.voter_id.compareTo(another.voter_id);
		}
	}

	public static String makeRollId(String chamber, int number, int year) {
        return normalizeRollId(chamber, String.valueOf(number), String.valueOf(year));
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
	
	// formattedNumber can be anything that ends with a number - the number will be extracted,
	// the chamber's first letter will be used, and combined into a roll ID
	public static String normalizeRollId(String chamber, String formattedNumber, String year) {
		String shortChamber;
		switch (chamber) {
			case "house":
				shortChamber = "h";
				break;
			case "senate":
				shortChamber = "s";
				break;
			default:
				return null;
		}
		
		String number = formattedNumber.replaceAll("[^\\d]", "");
		
		return shortChamber + number + "-" + year;
	}
}