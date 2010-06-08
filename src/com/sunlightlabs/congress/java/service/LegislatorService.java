package com.sunlightlabs.congress.java.service;

import java.util.ArrayList;

import org.json.JSONObject;

import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Legislator;

public interface LegislatorService {
	Legislator fromDrumbone(JSONObject json) throws CongressException;

	Legislator fromSunlight(JSONObject json) throws CongressException;

	ArrayList<Legislator> allWhere(String key, String value) throws CongressException;

	ArrayList<Legislator> allForZipCode(String zip) throws CongressException;

	ArrayList<Legislator> allForLatLong(double latitude, double longitude) throws CongressException;

	Legislator find(String bioguide_id) throws CongressException;

	Legislator legislatorFor(String url) throws CongressException;

	ArrayList<Legislator> legislatorsFor(String url) throws CongressException;

}
