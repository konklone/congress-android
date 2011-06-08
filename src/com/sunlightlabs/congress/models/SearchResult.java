package com.sunlightlabs.congress.models;

import java.util.ArrayList;
import java.util.Map;

public abstract class SearchResult {
	public double score;
	public Map<String,ArrayList<String>> highlight;
}