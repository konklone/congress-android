package com.sunlightlabs.entities;

import java.util.*;

import org.json.*;

import com.sunlightlabs.api.*;

/**
 * represents a registered Lobbyist 
 * 
 * com.sunlightlabs.entities.Legislator steve
 * Jul 22, 2009
 */
public class Lobbyist extends JSONEntity {
	public static Class<Lobbyist> THIS_CLASS = Lobbyist.class;
	public static Lobbyist[] EMPTY_ARRAY = {};

	public static final String[] KNOWN_PROPERTIES = { 
		"registrant_name", //=Bernstein Strategy Group
		"registrant_address", //=919 18th Street, NW
		"client_ppb_state", //DISTRICT OF COLUMBIA
		"registrant_description", //Lobbying and consulting firm speacializing in education, education technology and telecom.
		"filing_type", //=YEAR-END REPORT
		"registrant_ppb_country", //USA
		"filing_date", //2008-02-14
		"filing_pdf", //http://soprweb.senate.gov/index.cfm?event=printFiling&filingId=03404F3C-3084-4B2E-949F-0788E86E547F
		"registrant_country", //USA
		"client_contact_lastname", //BERNSTEIN
		"client_contact_firstname", //JON
		"client_ppb_country", //USA
		"client_country", //USA
		"filing_period", //H2
		"client_description", //unspecified
		"client_name", //SUNLIGHT FOUNDATION
		"client_state", //=DISTRICT OF COLUMBIA
		"filing_id", //=03404F3C-3084-4B2E-949F-0788E86E547F	
		
		 };

	public static final String name = "value";

	public static String getPluralEntityName() {
		return "filings";
	}

	/**
	 * internal function to build Lobbyists
	 * 
	 * @param items
	 *            non-null array of JSONObject
	 * @return non-null array of Lobbyist
	 */
	protected static Lobbyist[] buildLobbyists(JSONObject[] items) {
		Lobbyist[] ret = new Lobbyist[items.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = new Lobbyist(items[i]);
		}
		return ret;

	}

	/**
	 * 
	 * @param call
	 *            non-null caller
	 * @param clientName
	 * @return
	 */
	public static Lobbyist[] allLobbyistsFiledWith(ApiCall call,
			String clientName) {
		Map<String, String> params = new HashMap<String, String>();
		String apiCall = "lobbyists.getFilingList";
		params.put("client_name", clientName);
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,
				getPluralEntityName());
		return buildLobbyists(items);
	}

	/**
	 * 
	 * @param call
	 *            non-null caller
	 * @param zipcode
	 * @return
	 */
	public static Lobbyist[] getLobbyistsForZipCode(ApiCall call, String zipcode) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("zip", zipcode);
		String apiCall = "lobbyists.allForZip";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,
				getPluralEntityName());
		return buildLobbyists(items);
	}

	private Issue[] m_Issues;
	/**
	 * constructor
	 * 
	 * @param data
	 *            non-nulljson object with properties
	 */
	public Lobbyist(JSONObject data) {
		super(data);
	}

	/**
	 * constructor
	 * 
	 * @param data
	 *            non-null map with properties
	 */
	public Lobbyist(Map data) {
		super(data);
	}

	/**
	 * JSON Tag for this
	 */
	public String getEntityName() {
		return "filing";
	}

	/**
	 * unofficial list of properties
	 */
	public String[] getKnownProperties() {
		return KNOWN_PROPERTIES;
	}
	
	public String getName()
	{
		return getProperty("registrant_name");
	}
	
	protected void handleJSONArray( JSONArray value)
	{
		JSONObject[] subcommittees = getArrayItems(value);
		m_Issues = Issue.buildIssues(subcommittees);

	}


	public Issue[] getIssues() {
		if(m_Issues == null)
			return Issue.EMPTY_ARRAY;
		return m_Issues;
	}

	

	/**
	 * write all name value pairs to the appender
	 * @param out non-null appender
	 */
	@Override
	public  void showProperties(Appendable out) {
		super.showProperties( out);
		Issue[] values = getIssues();
		for (int j = 0; j < values.length; j++) {
			values[j].showProperties( out);
		}
	}
}
