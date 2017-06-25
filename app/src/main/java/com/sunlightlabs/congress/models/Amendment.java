package com.sunlightlabs.congress.models;

import java.io.Serializable;

public class Amendment implements Serializable {
    private static final long serialVersionUID = 2L;

    public String amendment_id, purpose, description, amends_bill_id;

    public static String description(Amendment amendment) {
        if (amendment.description != null && !amendment.description.equals(""))
            return amendment.description;
        else if (amendment.purpose != null && !amendment.purpose.equals(""))
            return amendment.purpose;
        else
            return "(no description)";
    }
}