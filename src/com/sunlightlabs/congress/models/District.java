package com.sunlightlabs.congress.models;

public class District {

    public static String BASE_URL = "http://theunitedstates.io/districts/";

    public static String urlForDistrict(String state, String district) {
        return BASE_URL + "cds/2012/" + state + "-" + district + "/shape.geojson";
    }

    public static String urlForState(String state) {
        return BASE_URL + "states/" + state + "/shape.geojson";
    }

    public static String urlForLegislator(Legislator legislator) {
        if (legislator.chamber.equals("senate"))
            return urlForState(legislator.state);
        else
            return urlForDistrict(legislator.state, legislator.district);
    }
}