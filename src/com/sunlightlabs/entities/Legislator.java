package com.sunlightlabs.entities;

import java.util.*;

import org.json.*;

import com.sunlightlabs.api.*;

/**
  * represents a  state legislator
* com.sunlightlabs.entities.Legislator steve Jul 22, 2009
 */
public class Legislator extends JSONEntity {
	public static Class<Legislator> THIS_CLASS = Legislator.class;
	public static Legislator[] EMPTY_ARRAY = {};

	public static final String[] KNOWN_PROPERTIES = { "webform", "phone",
			"govtrack_id", "state", "lastname", "party", "title", "gender",
			"district", "congresspedia_url", "senate_class", "fax",
			"middlename", "eventful_id", "website", "nickname", "votesmart_id",
			"crp_id", "congress_office", "firstname", "sunlight_old_id",
			"fec_id", "name_suffix", "twitter_id", "official_rss", "email",
			"youtube_url", "in_office", "bioguide_id" };

	public static final String name = "value";

	public static String getPluralEntityName() {
		return "legislators";
	}

	/**
	 * internal function to build Legislators
	 * @param items non-null array of JSONObject
	 * @return non-null array of Legislator
	 */
	protected static Legislator[] buildLegislators(JSONObject[] items) {
		Legislator[] ret = new Legislator[items.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = new Legislator(items[i]);
		}
		return ret;

	}

	/**
	 * return all Legislators in the database
	 * @param call non-null caller 
	 * @return non-null array of Legislators
	 */
	public static Legislator[] allLegislators(ApiCall call) {
		Map<String, String> params = new HashMap<String, String>();
		String apiCall = "legislators.getList";
		return allLegislators(call, params);

	}

	/**
	 * 
	 * @param call non-null caller 
	 * @param params non-null mapo of search properties
	 * @return non-null array of Legislators
		 */
	public static Legislator[] allLegislators(ApiCall call,
			Map<String, String> params) {
		String apiCall = "legislators.getList";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,
				getPluralEntityName());
		return buildLegislators(items);

	}

	/**
	 * 
	 * @param call non-null caller 
	 * @param zipcode
	 * @return
	 */
	public static Legislator[] getLegislatorsForZipCode(ApiCall call,
			String zipcode) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("zip", zipcode);
		String apiCall = "legislators.allForZip";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,
				getPluralEntityName());
		return buildLegislators(items);
	}

	/**
	 * Look up a Legislator by bioguide_id
	 * @param call non-null caller 
	 * @param id non-null bioguide_id
	 * @return possibly null Legislator
	 */
	public static Legislator getLegislatorById(ApiCall call, String id) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("bioguide_id", id);
		return getLegislatorById(call, params );
	}
	
	/**
	 * 
	 * @param call non-null caller
	 * @param params
	 * @return
	 */
	public static Legislator getLegislatorById(ApiCall call, Map<String, String> params ) {
		String apiCall = "legislators.get";
		JSONObject item = JSONEntity.getJSONObject(call, params, apiCall,
		"legislator");
		return new Legislator(item);
	}

	public static Legislator getLegislator(ApiCall call, String name) {
		Map<String, String> params = new HashMap<String, String>();
		String apiCall = "legislators.search";
		JSONObject item = JSONEntity.getJSONObject(call, params, apiCall,
				"legislator");
		return new Legislator(item);
	}

	/**
	 * constructor
	 * @param data non-nulljson object with properties
	 */
	public Legislator(JSONObject data) {
		super(data);
	}

	/**
	 * constructor
	 * @param data non-null map with properties
	 */
	public Legislator(Map data) {
		super(data);
	}

	/**
	 * JSON Tag for this
	 */
	public String getEntityName() {
		return "legislator";
	}

	/**
	 * unofficial list of properties
	 */
	public String[] getKnownProperties() {
		return KNOWN_PROPERTIES;
	}
	
	public String getName()
	{
		return getProperty("firstname") + " " + 
		getProperty("lastname");
	}
	
}
