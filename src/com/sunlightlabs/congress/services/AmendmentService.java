package com.sunlightlabs.congress.services;

import com.sunlightlabs.congress.models.Amendment;
import com.sunlightlabs.congress.models.CongressException;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class AmendmentService {
    protected static Amendment fromAPI(JSONObject json) throws JSONException, ParseException, CongressException {
        if (json == null)
            throw new CongressException("Error loading amendment.");

        Amendment amendment = new Amendment();

        if (!json.isNull("amendment_id"))
            amendment.amendment_id = json.getString("amendment_id");
        if (!json.isNull("description"))
            amendment.description = json.getString("description");
        if (!json.isNull("purpose"))
            amendment.purpose = json.getString("purpose");
        if (!json.isNull("amends_bill_id"))
            amendment.amends_bill_id = json.getString("amends_bill_id");

        return amendment;
    }
}
