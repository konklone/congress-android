package com.sunlightlabs.services;

import com.sunlightlabs.services.congress.CongressBillService;
import com.sunlightlabs.services.congress.CongressCommitteeService;
import com.sunlightlabs.services.congress.CongressLegislatorService;
import com.sunlightlabs.services.congress.CongressRollService;

public interface Services {
	public static LegislatorService legislators = new CongressLegislatorService();
	public static BillService bills = new CongressBillService();
	public static RollService rolls = new CongressRollService();
	public static CommitteeService committees = new CongressCommitteeService();
}
