package com.sunlightlabs.congress.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Hearing;

public class HearingService {

	public static List<Hearing> upcoming(String chamber, int page, int per_page) throws CongressException {
		Date now = new GregorianCalendar(DateUtils.GMT).getTime();
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("chamber", chamber);
		params.put("occurs_at__gte", RealTimeCongress.formatDate(now));
		params.put("committee__exists", "true");
		params.put("sort", "asc"); // start with the hearings closest to now
		
		if (chamber.equals("house"))
			params.put("dc", "true");
		
		return hearingsFor(RealTimeCongress.url("committee_hearings", null, params, page, per_page));
	}
	
	private static Hearing fromRTC(JSONObject json) throws JSONException, ParseException {
		Hearing hearing = new Hearing();
		
		if (!json.isNull("session"))
			hearing.session = json.getInt("session");
		if (!json.isNull("description"))
			hearing.description = json.getString("description");
		if (!json.isNull("chamber"))
			hearing.chamber = json.getString("chamber");
		if (!json.isNull("room"))
			hearing.room = json.getString("room");
		if (!json.isNull("occurs_at"))
			hearing.occursAt = RealTimeCongress.parseDate(json.getString("occurs_at"));
		
		if (!json.isNull("committee"))
			hearing.committee = CommitteeService.fromRTC(json.getJSONObject("committee"));
		
		return hearing;
	}
	
	private static List<Hearing> hearingsFor(String url) throws CongressException {
		String rawJSON = RealTimeCongress.fetchJSON(url);
		List<Hearing> hearings = new ArrayList<Hearing>();
		try {
			JSONArray results = new JSONObject(rawJSON).getJSONArray("committee_hearings");

			int length = results.length();
			for (int i = 0; i < length; i++)
				hearings.add(fromRTC(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return hearings;
	}
}