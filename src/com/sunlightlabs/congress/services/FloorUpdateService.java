package com.sunlightlabs.congress.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.FloorUpdate;

public class FloorUpdateService {
	
	private static String[] fields = new String[] {
		"update", "chamber", "congress",
		"legislative_day", "timestamp",
		"legislator_ids", "bill_ids", "roll_ids"
	};
	
	public static List<FloorUpdate> latest(String chamber, int page, int per_page) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("chamber", chamber);
		
		return updatesFor(Congress.url("floor_updates", fields, params, page, per_page));
	}
	
	protected static FloorUpdate fromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		FloorUpdate update = new FloorUpdate();
		
		if (!json.isNull("update"))
			update.update = Congress.unicode(json.getString("update"));
		
		if (!json.isNull("chamber"))
			update.chamber = json.getString("chamber");
		if (!json.isNull("congress"))
			update.congress = json.getInt("congress");
		
		if (!json.isNull("legislative_day"))
			update.legislativeDay = Congress.parseDateOnly(json.getString("legislative_day"));
		if (!json.isNull("timestamp"))
			update.timestamp = Congress.parseDate(json.getString("timestamp"));
		
		if (!json.isNull("legislator_ids"))
			update.legislatorIds = Congress.listFrom(json.getJSONArray("legislator_ids"));
		if (!json.isNull("bill_ids"))
			update.billIds = Congress.listFrom(json.getJSONArray("bill_ids"));
		if (!json.isNull("roll_ids"))
			update.rollIds = Congress.listFrom(json.getJSONArray("roll_ids"));
		
		return update;
	}
	
	private static List<FloorUpdate> updatesFor(String url) throws CongressException {
		List<FloorUpdate> updates = new ArrayList<FloorUpdate>();
		try {
			JSONArray results = Congress.resultsFor(url);

			int length = results.length();
			for (int i = 0; i < length; i++)
				updates.add(fromAPI(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return updates;
	}
}