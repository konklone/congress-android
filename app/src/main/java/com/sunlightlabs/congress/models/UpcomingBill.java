package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.Date;

public class UpcomingBill implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int congress;
	public Date legislativeDay;
	public String range;
	public String billId, chamber;
	public String description;
}