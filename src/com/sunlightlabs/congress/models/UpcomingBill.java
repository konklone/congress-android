package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class UpcomingBill implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int session;
	public Date legislativeDay;
	public String sourceUrl, sourceType;
	public String billId;
	public Bill bill;
	public String chamber;
	public List<String> context;
}