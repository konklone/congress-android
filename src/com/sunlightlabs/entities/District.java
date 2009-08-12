package com.sunlightlabs.entities;

import java.util.*;

import org.json.*;

import com.sunlightlabs.api.*;

/**
 * represents a Legislative (?or Congressional) district
* com.sunlightlabs.entities.District steve Jul 22, 2009
 */
public class District extends JSONEntity {
	public static Class<District> THIS_CLASS = District.class;
	public static District[] EMPTY_ARRAY = {};

	public static final String[] KNOWN_PROPERTIES = { "state", "number" };

	public static final String name = "value";
	
	public static String getPluralEntityName() {
		return "districts";
	}


	/**
	 * internal function to build districts
	 * @param items non-null array of JSONObject
	 * @return non-null array of District
	 */
	protected static District[] buildDistricts(JSONObject[] items) {
		District[] ret = new District[items.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = new District(items[i]);
		}
		return ret;

	}

	/**
	 * 
	 * @param call call non-null api caller
	 * @return non-null array of districts
	 */
	public static District[] allDistricts(ApiCall call) {
		Map<String, String> params = new HashMap<String, String>();
		String apiCall = "districts.getList";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,"districts");
		return buildDistricts(items);

	}

	/**
	 * get all districts in a zip code ( usually one)
	 * @param call call non-null api caller
	 * @param zipcode non-null zip code
	 * @return non-null array of districts
	 */
	public static District[] getDistrictsForZipCode(ApiCall call,String zipcode) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("zip", zipcode);
		String apiCall = "districts.getDistrictsFromZip";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,"districts");
		return buildDistricts(items);
	}
	
	/**
	 * return all zip codes in the district
	 * @param call call non-null api caller
	 * @param state non-null existing state
	 * @param district non-null existing district number
	 * @return non-null possibly empty array of zip codes
	 */
	public static String[] getZipsFromDistricts(ApiCall call,String state, String district)
	{
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("state", state);
		params.put("district", district);
		String apiCall = "districts.getZipsFromDistrict";
		String[] items = JSONEntity.getJSONStrings(call, params, apiCall,"zips");
		return items;
	}
	
	/**
	 * read a district from a lat lon
	 * @param call non-null api caller
	 * @param lat district latitude as a decimal string i.e "47.67898"
	 * @param lon district longitude as a decimal string i.e "-121.67453"
	 * @return possibly null district
	 */
	public static District getDistrictFromLatLon(ApiCall call,String lat, String lon)
	{
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("latitude", lat);
		params.put("longitude", lon);
		String apiCall = "districts.getZipsFromDistrict";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,getPluralEntityName());
		if(items.length > 0)
				return new District(items[0]);
		return null;
	}

	/**
	 * constructor
	 * @param data non-nulljson object with properties
	 */
	public District(JSONObject data) {
		super(data);
	}

	/**
	 * constructor
	 * @param data non-null map with properties
	 */
	public District(Map data) {
		super(data);
	}

	/**
	 * JSON Tag for this
	 */
	public String getEntityName() {
		return "district";
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

}
