package com.sunlightlabs.congress.java;

import java.util.ArrayList;


public class Committee implements Comparable<Committee> {

	public String id, name, chamber;
	
	public static ArrayList<Committee> forLegislator(String bioguide_id) {
		return null;
	}
	
	public static ArrayList<Legislator> legislatorsForCommittee(String committeeId) {
		return null;
	}
	
	public int compareTo(Committee another) {
		return this.name.compareTo(another.name);
	}
	
	public String toString() {
		return name;
	}
	
}