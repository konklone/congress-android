package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.List;

public class Committee implements Comparable<Committee>, Serializable {
	private static final long serialVersionUID = 1L;

	public String id, name, chamber;
	
	public boolean subcommittee;
	public String parent_committee_id; 
	
	public List<Legislator> members;
	
	@Override
	public String toString() {
		return name;
	}
	
	public static class Membership implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public Legislator member;
		public String side, title;
		public int rank;
		
		public Membership() {};
	}
	
	@Override
	public int compareTo(Committee another) {
		String mine = name.replace("the ", "");
		String other = another.name.replace("the ", "");
		return mine.compareTo(other);
	}
}