package com.sunlightlabs.congress.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Hearing;

public class HearingService {

    public static String datetimeFormat = "yyyy-MM-dd hh:mm:ss";

	// /{congress}/committees/hearings.json
	public static List<Hearing> upcoming(int page) throws CongressException {
        String congress = String.valueOf(Bill.currentCongress());
        String[] endpoint = { congress, "committees", "hearings" };
		return hearingsFor(ProPublica.url(endpoint, page));
	}
	
	private static Hearing fromJSON(JSONObject json) throws JSONException, ParseException {
		Hearing hearing = new Hearing();
		
		if (!json.isNull("description"))
			hearing.description = json.getString("description");
		if (!json.isNull("chamber"))
			hearing.chamber = json.getString("chamber").toLowerCase();
		if (!json.isNull("location"))
			hearing.room = json.getString("location");

        if (!json.isNull("date") && !json.isNull("time")) {
            String timestamp = json.getString("date") + " " + json.getString("time");
            SimpleDateFormat format = new SimpleDateFormat(datetimeFormat);
            format.setTimeZone(ProPublica.CONGRESS_TIMEZONE);
            hearing.occursAt = format.parse(timestamp);
        }

		// House only
        // TODO: remove the empty string check after this is addressed:
        // https://github.com/propublica/congress-api-docs/issues/36#issuecomment-318854367
		if (!json.isNull("url") && !json.getString("url").equals(""))
			hearing.url = json.getString("url");
		if (!json.isNull("meeting_type") && !json.getString("meeting_type").equals(""))
			hearing.hearingType = json.getString("meeting_type");
		
		if (!json.isNull("committee")) {
            Committee committee = new Committee();
            committee.name = Utils.decodeHTML(json.getString("committee"));
            committee.id = json.getString("committee_code");
            committee.chamber = hearing.chamber;
            hearing.committee = committee;
        }
		
		return hearing;
	}
	
	private static List<Hearing> hearingsFor(String url) throws CongressException {
		List<Hearing> hearings = new ArrayList<Hearing>();
		try {
			JSONArray results = ProPublica.resultsFor(url);

            if (results.length() > 0) {
                JSONObject firstResult = results.getJSONObject(0);
                if (!firstResult.isNull("hearings"))
                    results = firstResult.getJSONArray("hearings");
            }

			int length = results.length();
			for (int i = 0; i < length; i++)
				hearings.add(fromJSON(results.getJSONObject(i)));
		} catch(JSONException e) {
			throw new CongressException(e, "Problem parsing the hearings at " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a hearing date from " + url);
		}
		
		return hearings;
	}
}