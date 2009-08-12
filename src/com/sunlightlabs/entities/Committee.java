package com.sunlightlabs.entities;

import java.io.*;
import java.util.*;

import org.json.*;

import com.sunlightlabs.api.*;

/**
 * represents a Committee in congress or a legislature
 * com.sunlightlabs.entities.Legislator steve Jul 22, 2009
 */
public class Committee extends JSONEntity {
	public static Class<Committee> THIS_CLASS = Committee.class;
	public static Committee[] EMPTY_ARRAY = {};

	public static final String[] KNOWN_PROPERTIES = { "chamber", "id",
				"name", "subcommittees" };

	public static final String name = "value";


	public static String getPluralEntityName() {
		return "committees";
	}

	/**
	 * internal function to build Committees
	 * @param items non-null array of JSONObject
	 * @return non-null array of Committee
	 */
	protected static Committee[] buildCommittees(JSONObject[] items) {
		Committee[] ret = new Committee[items.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = new Committee(items[i]);
		}
		return ret;

	}

	/**
	 * 
	 * @param call non-null caller
	 * @param chamber
	 * @return non-null array of committees
	 */
	public static Committee[] allCommittees(ApiCall call,String chamber) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("chamber", chamber);
		String apiCall = "committees.getList";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,getPluralEntityName());
		return buildCommittees(items);

	}

	/**
	 * 
	 * 
	 * @param call non-null caller
	 * @param id
	 * @return
	 */
	public static Committee getCommitteeById(ApiCall call, String id ) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("id", id);
		return getCommitteeById( call,  params );
	}

	/**
	 * 
	 * @param call non-null caller
	 * @param params
	 * @return possiblu null Commottee
	 */
	public static Committee getCommitteeById(ApiCall call, Map<String, String> params ) {
		String apiCall = "committees.get";
		JSONObject item = JSONEntity.getJSONObject(call, params, apiCall,
				getPluralEntityName());
		return new Committee(item);
	}



	/**
	 * 
	 * @param call non-null caller
	 * @param biocode
	 * @return non-null array of committees
		 */
	public static Committee[] getCommitteesForLegislator(ApiCall call,Legislator leg) {
		 return getCommitteesForLegislator( call,leg.getProperty("bioguide_id"));
	}
	/**
	 * 
	 * @param call non-null caller
	 * @param biocode
	 * @return non-null array of committees
		 */
	public static Committee[] getCommitteesForLegislator(ApiCall call,String biocode) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("bioguide_id", biocode);
		String apiCall = "committees.allForLegislator";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,getPluralEntityName());
		return buildCommittees(items);
	}

	
	private Committee[] m_Subcommittees = EMPTY_ARRAY;
	
	/**
	 * constructor
	 * @param data non-null json object with properties
	 */
	public Committee(JSONObject data) {
		super(data);
	}

	/**
	 * constructor
	 * @param data non-null map with properties
	 */
	public Committee(Map data) {
		super(data);
	}
	
	protected void handleJSONArray( JSONArray value)
	{
		JSONObject[] subcommittees = getArrayItems(value);
		m_Subcommittees = buildCommittees(subcommittees);
	}

	
	public Committee[] getSubcommittees() {
		return m_Subcommittees;
	}

	public void setSubcommittees(Committee[] subcommittees) {
		m_Subcommittees = subcommittees;
	}

	/**
	 * JSON Tag for this
	 */
	public String getEntityName() {
		return "committee";
	}

	/**
	 * unofficial list of properties
	 */
	public String[] getKnownProperties() {
		return KNOWN_PROPERTIES;
	}
	
	public String getName()
	{
		return getProperty("name");
	}
	
	/**
	 * write all name value pairs to the appender
	 * @param out non-null appender
	 */
	@Override
	public  void showProperties(Appendable out) {
		super.showProperties( out);
		Committee[] values = getSubcommittees();
		for (int j = 0; j < values.length; j++) {
			values[j].showProperties( out);
		}
	}

}
