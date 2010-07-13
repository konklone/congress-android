package com.sunlightlabs.congress.models;

import java.util.ArrayList;
import java.util.Collections;

public class Committee implements Comparable<Committee> {

	public String id, name, chamber;
	public ArrayList<Legislator> members;
	
	public int compareTo(Committee another) {
		String mine = name.replace("the ", "");
		String other = another.name.replace("the ", "");
		return mine.compareTo(other);
	}
	
	public String toString() {
		return name;
	}
	
}