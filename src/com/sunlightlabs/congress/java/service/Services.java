package com.sunlightlabs.congress.java.service;

public class Services {
	public static LegislatorService legislator = new CongressLegislatorService();
	public static BillService bill = new CongressBillService();
	public static RollService roll = new CongressRollService();
}
