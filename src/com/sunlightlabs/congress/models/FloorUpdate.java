package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class FloorUpdate implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public List<String> billIds, rollIds, legislatorIds;
	public Date timestamp;
	public Date legislativeDay;
	public List<String> events;
	public String chamber;
	public int session;
}