package com.sunlightlabs.congress.models;

import java.util.ArrayList;

public class Committee implements Comparable<Committee> {

	public String id, name, chamber;
	public ArrayList<Legislator> members;
	
	public int compareTo(Committee another) {
		return this.name.compareTo(another.name);
	}
	
	public String toString() {
		return name;
	}
	
}