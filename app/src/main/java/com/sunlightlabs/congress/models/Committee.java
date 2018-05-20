package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.List;

public class Committee implements Comparable<Committee>, Serializable {
	private static final long serialVersionUID = 1L;

	public String id, name, chamber;
	public String url;
	
	public boolean subcommittee;
	public String parent_committee_id, parent_committee_name;

    public List<Committee> subcommittees;

    public Legislator chair;
	public List<Legislator> members;
	
	public String toString() {
		return name;
	}
	
	public static class Membership implements Serializable {
		private static final long serialVersionUID = 1L;

		public String side, title;
		public int rank;
		
		public Membership() {}
    }
	
	public int compareTo(Committee another) {
		String mine = name.replace("the ", "");
		String other = another.name.replace("the ", "");
		return mine.compareTo(other);
	}
}