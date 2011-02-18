package com.sunlightlabs.congress.models;

import java.util.Date;
import java.util.List;

public class FloorUpdate {
	public List<String> billIds, rollIds, legislatorIds;
	public Date timestamp;
	public Date legislativeDay;
	public List<String> events;
	public String chamber;
}