package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Nomination;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class NominationService {

    protected static Nomination fromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
        if (json == null)
            throw new CongressException("Error loading nomination.");

        Nomination nomination = new Nomination();

        if (!json.isNull("number"))
            nomination.number = json.getString("number");
        if (!json.isNull("organization"))
            nomination.organization = json.getString("organization");
        if (!json.isNull("nomination_id"))
            nomination.nomination_id = json.getString("nomination_id");

        if (!json.isNull("nominees")) {
            JSONArray nomineesObject = json.getJSONArray("nominees");
            int count = nomineesObject.length();
            List<Nomination.Nominee> nominees = new ArrayList<Nomination.Nominee>(count);

            for (int i=0; i<count; i++) {
                JSONObject nomineeObject = nomineesObject.getJSONObject(i);
                Nomination.Nominee nominee = new Nomination.Nominee();

                if (!nomineeObject.isNull("name"))
                    nominee.name = nomineeObject.getString("name");
                if (!nomineeObject.isNull("position"))
                    nominee.position = nomineeObject.getString("position");
                if (!nomineeObject.isNull("state"))
                    nominee.state= nomineeObject.getString("state");
                nominees.add(nominee);
            }
            nomination.nominees = nominees;
        }

        return nomination;
    }
}
