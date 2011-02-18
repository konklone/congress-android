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
	
	public static List<FloorUpdate> latest(String chamber, int page, int per_page) throws CongressException {
		String[] sections = new String[] {"basic"};
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("chamber", chamber);
		
		return updatesFor(RealTimeCongress.url("floor_updates", sections, params, page, per_page));
	}
	
	protected static FloorUpdate fromRTC(JSONObject json) throws JSONException, ParseException {
		FloorUpdate update = new FloorUpdate();
		
		if (!json.isNull("events"))
			update.events = listFrom(json.getJSONArray("events"));
		
		if (!json.isNull("chamber"))
			update.chamber = json.getString("chamber");
		
		if (!json.isNull("legislative_day"))
			update.legislativeDay = RealTimeCongress.parseDateOnly(json.getString("legislative_day"));
		
		if (!json.isNull("timestamp"))
			update.timestamp = RealTimeCongress.parseDate(json.getString("timestamp"));
		
		if (!json.isNull("legislator_ids"))
			update.legislatorIds = listFrom(json.getJSONArray("legislator_ids"));
		
		if (!json.isNull("bill_ids"))
			update.billIds = listFrom(json.getJSONArray("bill_ids"));
		
		if (!json.isNull("roll_ids"))
			update.rollIds = listFrom(json.getJSONArray("roll_ids"));
		
		return update;
	}
	
	private static List<String> listFrom(JSONArray array) throws JSONException {
		int length = array.length();
		List<String> list = new ArrayList<String>(length);
		
		for (int i=0; i<length; i++)
			list.add(array.getString(i));
		
		return list;
	}
	
	private static List<FloorUpdate> updatesFor(String url) throws CongressException {
		String rawJSON = RealTimeCongress.fetchJSON(url);
		List<FloorUpdate> updates = new ArrayList<FloorUpdate>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("floor_updates");

			int length = results.length();
			for (int i = 0; i < length; i++)
				updates.add(fromRTC(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return updates;
	}
}