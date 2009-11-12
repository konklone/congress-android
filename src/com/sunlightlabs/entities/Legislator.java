package com.sunlightlabs.entities;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.sunlightlabs.api.ApiCall;

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
	
	public String toString() {
		return titledName() + " [" + getSeatSuffix() + "]";
	}
	
	public String getId() {
		return getProperty("bioguide_id");
	}
	
	public String getName() {
		return firstName() + " " + lastName();
	}
	
	public String titledName() {
		String name = getProperty("title") + ". " + getName();
		String suffix = getProperty("name_suffix");
		if (suffix != null && !suffix.equals(""))
			name += " " + suffix;
		return name;
	}
	
	public String youtubeUsername() {
		String url = getProperty("youtube_url");
		Pattern p = Pattern.compile("http://(?:www\\.)?youtube\\.com/(?:user/)?(.*?)/?$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(url);
		boolean found = m.find();
		if (found)
			return m.group(1);
		else
			return "";
	}
	
	public String firstName() {
		String first_name = getProperty("nickname");
		if (first_name == null || first_name.length() == 0)
			first_name = getProperty("firstname");
		return first_name;
	}
	
	public String lastName() {
		return getProperty("lastname");
	}
	
	public String getDomain() {
		String district = getProperty("district");
		if (district.equals("Senior Seat") || district.equals("Junior Seat"))
			return district;
		else if (district.equals("0"))
			return "At-Large";
		else
			return "District " + district;
	}
	
	public String getSeatSuffix() {
		String suffix = getProperty("state") + "-";
		String district = getProperty("district");
		if (district.equals("Senior Seat"))
			suffix += "Senior";
		else if (district.equals("Junior Seat"))
			suffix += "Junior";
		else if (district.equals("0"))
			suffix += "At-Large";
		else
			suffix += district;
		return suffix;
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
	public static Legislator[] allLegislators(ApiCall call) throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		String apiCall = "legislators.getList";
		return allLegislators(call, params);
	}

	/**
	 * 
	 * @param call non-null caller 
	 * @param params non-null map of search properties
	 * @return non-null array of Legislators
		 */
	public static Legislator[] allLegislators(ApiCall call,
			Map<String, String> params) throws IOException {
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
			String zipcode) throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("zip", zipcode);
		String apiCall = "legislators.allForZip";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall,
				getPluralEntityName());
		return buildLegislators(items);
	}
	
	public static Legislator[] getLegislatorsForLatLong(ApiCall call, String latitude, String longitude) throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("latitude", latitude);
		params.put("longitude", longitude);
		String apiCall = "legislators.allForLatLong";
		JSONObject[] items = JSONEntity.getJSONObjects(call, params, apiCall, getPluralEntityName());
		return buildLegislators(items);
	}
	
	public static Legislator[] getLegislatorsForLatLong(ApiCall call, double latitude, double longitude) throws IOException {
		return getLegislatorsForLatLong(call, latitude + "", longitude + "");
	}

	/**
	 * Look up a Legislator by bioguide_id
	 * @param call non-null caller 
	 * @param id non-null bioguide_id
	 * @return possibly null Legislator
	 */
	public static Legislator getLegislatorById(ApiCall call, String id) throws IOException {
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
	public static Legislator getLegislatorById(ApiCall call, Map<String, String> params ) throws IOException {
		String apiCall = "legislators.get";
		JSONObject item = JSONEntity.getJSONObject(call, params, apiCall,
		"legislator");
		return new Legislator(item);
	}

	public static Legislator getLegislator(ApiCall call, String name) throws IOException {
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
	
	
}
