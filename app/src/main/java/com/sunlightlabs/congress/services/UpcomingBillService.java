package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.UpcomingBill;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class UpcomingBillService {

    // /bills/upcoming/{chamber}.json
	public static List<UpcomingBill> comingUp() throws CongressException {
        String[] house = {"bills", "upcoming", "house" };
        return upcomingBillsFor(ProPublica.url(house));
	}
	
	protected static UpcomingBill fromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
		UpcomingBill upcoming = new UpcomingBill();

        if (!json.isNull("congress"))
            upcoming.congress = Integer.valueOf(json.getString("congress"));
		
		if (!json.isNull("chamber"))
			upcoming.chamber = json.getString("chamber").toLowerCase();
		
		// field can be present but actually null 
		if (!json.isNull("legislative_day"))
			upcoming.legislativeDay = ProPublica.parseDateOnly(json.getString("legislative_day"));
		
		if (!json.isNull("range"))
			upcoming.range = json.getString("range").toLowerCase();

        if (!json.isNull("bill_id")) {
            upcoming.billId = json.getString("bill_id");

            // failsafe against "H.R. ____" or other changes
            if (Bill.splitBillId(upcoming.billId) == null)
                upcoming.billId = null;
        }

        if (!json.isNull("description"))
            upcoming.description = json.getString("description").trim();

		return upcoming;
	}
	
	private static List<UpcomingBill> upcomingBillsFor(String url) throws CongressException {
		List<UpcomingBill> upcomings = new ArrayList<UpcomingBill>();
		try {
			JSONArray results = ProPublica.resultsFor(url);

            results = results.getJSONObject(0).getJSONArray("bills");

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