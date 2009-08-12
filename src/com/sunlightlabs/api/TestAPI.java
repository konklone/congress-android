package com.sunlightlabs.api;

import com.sunlightlabs.entities.*;

/**
 * This class has a main to exercise the API
 * com.sunlightlabs.api.TestAPI
 * steve Jul 23, 2009
 */
public class TestAPI {
	public static Class<TestAPI> THIS_CLASS = TestAPI.class;
	public static TestAPI[] EMPTY_ARRAY = {};
	
	public static final String DEFAULT_API_KEY = "0bb288364a2754e5a0f49b1eed69091e";


	

	/**
	 * test main
	 * @param args ignores
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ApiCall me = new ApiCall(DEFAULT_API_KEY);
		
		Legislator[] legislators = null;
		System.out.println("Reading All Legislators");
		legislators = Legislator.allLegislators(me);
		for (int i = 0; i < Math.min(5,legislators.length); i++) {
			Legislator l = legislators[i];
			l.show();
			System.out.println();
	}
		System.out.println("===============================================");

		System.out.println("Reading All House Committees");
		Committee[] committees = Committee.allCommittees(me,"House");
		for (int i = 0; i < Math.min(5,committees.length); i++) {
			Committee l = committees[i];
			l.show();
			System.out.println();
	}
				
		System.out.println("===============================================");

		Legislator testLegislator = legislators[0];
		System.out.println("Reading All  Committees for " + testLegislator.getName());
		
		committees = Committee.getCommitteesForLegislator(me,testLegislator);
		for (int i = 0; i < Math.min(5,committees.length); i++) {
			Committee l = committees[i];
			l.show();
			System.out.println();
		}
				
		System.out.println("===============================================");

		String zip = "98033";
		District[] districts = District.getDistrictsForZipCode(me,zip);
		District dist  = districts[0];
		String[] zips = District.getZipsFromDistricts(me, "WA", dist.getProperty("number"));
		boolean foundLocalZip = false;
		for (int i = 0; i < zips.length; i++) {
			if(zip.equals(zips[i])) {
				foundLocalZip = true;
				break;
			}
		}
		if(!foundLocalZip)
			System.out.println("Could not fine locating zipcode");
		
		System.out.println("===============================================");
		
		String registeree = "Sunlight Foundation";
		System.out.println("Reading All  Lobbiests registered with " + registeree);
		
		Lobbyist[] lobbiests = Lobbyist.allLobbyistsFiledWith(me,registeree);
		for (int i = 0; i < Math.min(5,lobbiests.length); i++) {
			Lobbyist l = lobbiests[i];
			l.show();
			System.out.println();

		}
				
		System.out.println("===============================================");


	}


}
