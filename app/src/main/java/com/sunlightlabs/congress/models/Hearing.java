package com.sunlightlabs.congress.models;

import java.util.Date;

public class Hearing {
	public String chamber;
	public Date occursAt;
	public Committee committee;
	public String description;
	public String room;

	// House only
	public String url;
	public String hearingType;
}