package com.sunlightlabs.congress.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.FloorUpdate;

public class FloorUpdateService {

    // e.g. "2017-05-15 10:02:43 -0400",
    public static String datetimeFormat = "yyyy-MM-dd hh:mm:ss Z";

	// /{congress}/{chamber}/floor_updates.json
	public static List<FloorUpdate> latest(String chamber, int page) throws CongressException {
		String congress = String.valueOf(Bill.currentCongress());
		String[] endpoint = { congress, chamber, "floor_updates" };

		return updatesFor(ProPublica.url(endpoint, page));
	}
	
	protected static FloorUpdate fromAPI(JSONObject json) throws JSONException, ParseException {
		FloorUpdate update = new FloorUpdate();

        if (!json.isNull("description"))
			update.update = json.getString("description");

		if (!json.isNull("congress"))
			update.congress = json.getInt("congress");

		if (!json.isNull("chamber"))
		    update.chamber = json.getString("chamber").toLowerCase();

		if (!json.isNull("date"))
			update.legislativeDay = ProPublica.parseDateOnly(json.getString("date"));
		if (!json.isNull("timestamp"))
			update.timestamp = ProPublica.parseTimestamp(json.getString("timestamp"), FloorUpdateService.datetimeFormat);

		return update;
	}
	
	private static List<FloorUpdate> updatesFor(String url) throws CongressException {
		List<FloorUpdate> updates = new ArrayList<FloorUpdate>();
		try {
			JSONArray results = ProPublica.resultsFor(url);

            if (results.length() > 0) {
                JSONObject firstResult = results.getJSONObject(0);
                if (!firstResult.isNull("floor_actions"))
                    results = firstResult.getJSONArray("floor_actions");
            }

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