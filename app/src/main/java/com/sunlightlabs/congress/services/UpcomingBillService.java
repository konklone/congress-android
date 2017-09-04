package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.UpcomingBill;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class UpcomingBillService {

	public static List<UpcomingBill> comingUp() throws CongressException {
		return null;
	}
	
	protected static UpcomingBill fromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		UpcomingBill upcoming = new UpcomingBill();
		
		if (!json.isNull("context"))
			upcoming.context = json.getString("context");
		
		if (!json.isNull("chamber"))
			upcoming.chamber = json.getString("chamber");
		
		// field can be present but actually null 
		if (!json.isNull("legislative_day"))
			upcoming.legislativeDay = null; // Congress.parseDateOnly(json.getString("legislative_day"));
		
		if (!json.isNull("range"))
			upcoming.range = json.getString("range");
		
		if (!json.isNull("bill_id"))
			upcoming.billId = json.getString("bill_id");
		
		if (!json.isNull("source_type"))
			upcoming.sourceType = json.getString("source_type");
		
		if (!json.isNull("url"))
			upcoming.sourceUrl = json.getString("url");
		
		if (!json.isNull("congress"))
			upcoming.congress = json.getInt("congress");
		
		return upcoming;
	}
	
	private static List<UpcomingBill> upcomingBillsFor(String url) throws CongressException {
		List<UpcomingBill> upcomings = new ArrayList<UpcomingBill>();
		try {
			JSONArray results = ProPublica.resultsFor(url);

			int length = results.length();
			for (int i = 0; i < length; i++)
				upcomings.add(fromAPI(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return upcomings;
	}
}