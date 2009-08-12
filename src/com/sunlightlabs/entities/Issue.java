package com.sunlightlabs.entities;

import java.util.*;

import org.json.*;

import com.sunlightlabs.api.*;

/**
 * represents a Lobbyist issue
* com.sunlightlabs.entities.Issue steve Jul 22, 2009
 */
public class Issue extends JSONEntity {
	public static Class<Issue> THIS_CLASS = Issue.class;
	public static Issue[] EMPTY_ARRAY = {};

	public static final String[] KNOWN_PROPERTIES = { "code", "specific_issue" };

	public static final String name = "value";
	
	public static String getPluralEntityName() {
		return "issues";
	}


	/**
	 * internal function to build districts
	 * @param items non-null array of JSONObject
	 * @return non-null array of District
	 */
	protected static Issue[] buildIssues(JSONObject[] items) {
		Issue[] ret = new Issue[items.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = new Issue(items[i]);
		}
		return ret;

	}



	/**
	 * constructor
	 * @param data non-nulljson object with properties
	 */
	public Issue(JSONObject data) {
		super(data);
	}

	/**
	 * constructor
	 * @param data non-null map with properties
	 */
	public Issue(Map data) {
		super(data);
	}

	/**
	 * JSON Tag for this
	 */
	public String getEntityName() {
		return "issue";
	}
	

	/**
	 * unofficial list of properties
	 */

	public String[] getKnownProperties() {
		return KNOWN_PROPERTIES;
	}
	
	public String getName()
	{
		return getProperty("code");
	}

}
