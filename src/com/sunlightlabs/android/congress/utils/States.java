package com.sunlightlabs.android.congress.utils;


public class States {
	
	public static String codeToName(String code) {
		if (code.equals("AL"))
	        return "Alabama";
	    if (code.equals("AK"))
	        return "Alaska";
	    if (code.equals("AZ"))
	        return "Arizona";
	    if (code.equals("AR"))
	        return "Arkansas";
	    if (code.equals("CA"))
	        return "California";
	    if (code.equals("CO"))
	        return "Colorado";
	    if (code.equals("CT"))
	        return "Connecticut";
	    if (code.equals("DE"))
	        return "Delaware";
	    if (code.equals("DC"))
	        return "District of Columbia";
	    if (code.equals("FL"))
	        return "Florida";
	    if (code.equals("GA"))
	        return "Georgia";
	    if (code.equals("HI"))
	        return "Hawaii";
	    if (code.equals("ID"))
	        return "Idaho";
	    if (code.equals("IL"))
	        return "Illinois";
	    if (code.equals("IN"))
	        return "Indiana";
	    if (code.equals("IA"))
	        return "Iowa";
	    if (code.equals("KS"))
	        return "Kansas";
	    if (code.equals("KY"))
	        return "Kentucky";
	    if (code.equals("LA"))
	        return "Louisiana";
	    if (code.equals("ME"))
	        return "Maine";
	    if (code.equals("MD"))
	        return "Maryland";
	    if (code.equals("MA"))
	        return "Massachusetts";
	    if (code.equals("MI"))
	        return "Michigan";
	    if (code.equals("MN"))
	        return "Minnesota";
	    if (code.equals("MS"))
	        return "Mississippi";
	    if (code.equals("MO"))
	        return "Missouri";
	    if (code.equals("MT"))
	        return "Montana";
	    if (code.equals("NE"))
	        return "Nebraska";
	    if (code.equals("NV"))
	        return "Nevada";
	    if (code.equals("NH"))
	        return "New Hampshire";
	    if (code.equals("NJ"))
	        return "New Jersey";
	    if (code.equals("NM"))
	        return "New Mexico";
	    if (code.equals("NY"))
	        return "New York";
	    if (code.equals("NC"))
	        return "North Carolina";
	    if (code.equals("ND"))
	        return "North Dakota";
	    if (code.equals("OH"))
	        return "Ohio";
	    if (code.equals("OK"))
	        return "Oklahoma";
	    if (code.equals("OR"))
	        return "Oregon";
	    if (code.equals("PA"))
	        return "Pennsylvania";
	    if (code.equals("PR"))
	        return "Puerto Rico";
	    if (code.equals("RI"))
	        return "Rhode Island";
	    if (code.equals("SC"))
	        return "South Carolina";
	    if (code.equals("SD"))
	        return "South Dakota";
	    if (code.equals("TN"))
	        return "Tennessee";
	    if (code.equals("TX"))
	        return "Texas";
	    if (code.equals("UT"))
	        return "Utah";
	    if (code.equals("VT"))
	        return "Vermont";
	    if (code.equals("VA"))
	        return "Virginia";
	    if (code.equals("WA"))
	        return "Washington";
	    if (code.equals("WV"))
	        return "West Virginia";
	    if (code.equals("WI"))
	        return "Wisconsin";
	    if (code.equals("WY"))
	        return "Wyoming";
	    else
	        return null;
	}
	
	public static String nameToCode(String name) {
		name = name.toLowerCase();
	    if (name.equals("alabama"))
	        return "AL";
	    if (name.equals("alaska"))
	        return "AK";
	    if (name.equals("arizona"))
	        return "AZ";
	    if (name.equals("arkansas"))
	        return "AR";
	    if (name.equals("california"))
	        return "CA";
	    if (name.equals("colorado"))
	        return "CO";
	    if (name.equals("connecticut"))
	        return "CT";
	    if (name.equals("delaware"))
	        return "DE";
	    if (name.equals("district of columbia"))
	        return "DC";
	    if (name.equals("florida"))
	        return "FL";
	    if (name.equals("georgia"))
	        return "GA";
	    if (name.equals("hawaii"))
	        return "HI";
	    if (name.equals("idaho"))
	        return "ID";
	    if (name.equals("illinois"))
	        return "IL";
	    if (name.equals("indiana"))
	        return "IN";
	    if (name.equals("iowa"))
	        return "IA";
	    if (name.equals("kansas"))
	        return "KS";
	    if (name.equals("kentucky"))
	        return "KY";
	    if (name.equals("louisiana"))
	        return "LA";
	    if (name.equals("maine"))
	        return "ME";
	    if (name.equals("maryland"))
	        return "MD";
	    if (name.equals("massachusetts"))
	        return "MA";
	    if (name.equals("michigan"))
	        return "MI";
	    if (name.equals("minnesota"))
	        return "MN";
	    if (name.equals("mississippi"))
	        return "MS";
	    if (name.equals("missouri"))
	        return "MO";
	    if (name.equals("montana"))
	        return "MT";
	    if (name.equals("nebraska"))
	        return "NE";
	    if (name.equals("nevada"))
	        return "NV";
	    if (name.equals("new hampshire"))
	        return "NH";
	    if (name.equals("new jersey"))
	        return "NJ";
	    if (name.equals("new mexico"))
	        return "NM";
	    if (name.equals("new york"))
	        return "NY";
	    if (name.equals("north carolina"))
	        return "NC";
	    if (name.equals("north dakota"))
	        return "ND";
	    if (name.equals("ohio"))
	        return "OH";
	    if (name.equals("oklahoma"))
	        return "OK";
	    if (name.equals("oregon"))
	        return "OR";
	    if (name.equals("pennsylvania"))
	        return "PA";
	    if (name.equals("puerto rico"))
	        return "PR";
	    if (name.equals("rhode island"))
	        return "RI";
	    if (name.equals("south carolina"))
	        return "SC";
	    if (name.equals("south dakota"))
	        return "SD";
	    if (name.equals("tennessee"))
	        return "TN";
	    if (name.equals("texas"))
	        return "TX";
	    if (name.equals("utah"))
	        return "UT";
	    if (name.equals("vermont"))
	        return "VT";
	    if (name.equals("virginia"))
	        return "VA";
	    if (name.equals("washington"))
	        return "WA";
	    if (name.equals("west virginia"))
	        return "WV";
	    if (name.equals("wisconsin"))
	        return "WI";
	    if (name.equals("wyoming"))
	        return "WY";
	    else
	        return null;
	}
}