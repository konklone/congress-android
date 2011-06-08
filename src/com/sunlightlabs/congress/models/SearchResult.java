package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

public abstract class SearchResult implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public double score;
	public Map<String,ArrayList<String>> highlight;
	public String query;
}