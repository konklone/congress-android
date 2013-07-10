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
		params.put("occurs_at__gte", Congress.formatDate(now));
		params.put("committee__exists", "true"); // the new API should require this, but just in case
		params.put("order", "occurs_at__asc"); // start with the hearings closest to now
		
		params.put("dc", "true"); // some House hearings can take place in the field
		
		String[] fields = new String[] { 
			"chamber", "occurs_at", "committee", "committee_id", "congress", "url", 
			"room", "hearing_type", "description", "dc" 
		};
		
		return hearingsFor(Congress.url("hearings", fields, params, page, per_page));
	}
	
	private static Hearing fromJSON(JSONObject json) throws JSONException, ParseException, CongressException {
		Hearing hearing = new Hearing();
		
		if (!json.isNull("congress"))
			hearing.congress = json.getInt("congress");
		if (!json.isNull("description"))
			hearing.description = json.getString("description");
		if (!json.isNull("chamber"))
			hearing.chamber = json.getString("chamber");
		if (!json.isNull("room"))
			hearing.room = json.getString("room");
		if (!json.isNull("occurs_at"))
			hearing.occursAt = Congress.parseDate(json.getString("occurs_at"));
		if (!json.isNull("dc"))
			hearing.dc = json.getBoolean("dc");
		
		// House only
		if (!json.isNull("url"))
			hearing.url = json.getString("url");
		if (!json.isNull("hearing_type"))
			hearing.hearingType = json.getString("hearing_type");
		
		if (!json.isNull("committee"))
			hearing.committee = CommitteeService.fromAPI(json.getJSONObject("committee"));
		
		return hearing;
	}
	
	private static List<Hearing> hearingsFor(String url) throws CongressException {
		List<Hearing> hearings = new ArrayList<Hearing>();
		try {
			JSONArray results = Congress.resultsFor(url);

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