package com.sunlightlabs.congress.models;

import java.util.List;

public class Committee implements Comparable<Committee> {

	public String id, name, chamber;
	public List<Legislator> members;
	public List<Membership> memberships;
	
	public int compareTo(Committee another) {
		String mine = name.replace("the ", "");
		String other = another.name.replace("the ", "");
		return mine.compareTo(other);
	}
	
	public String toString() {
		return name;
	}
	
	public static class Membership {
		public Legislator member;
		public String rank, side, title;
		
		public Membership() {};
	}
}