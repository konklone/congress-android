package com.sunlightlabs.congress.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.District;
import com.sunlightlabs.congress.models.Legislator;

import org.geojson.GeoJsonObject;
import org.geojson.MultiPolygon;

import java.io.IOException;

public class DistrictService {

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

    // uses a special Jackson-based GeoJSON parser, not the typical JSONObject-based parser
    // https://github.com/opendatalab-de/geojson-jackson
    public static District find(Legislator legislator) throws CongressException {
        String url = urlForLegislator(legislator);
        String json = Congress.fetchJSON(url);

        // parse and return the given multipolygon
        try {
            GeoJsonObject geojson = new ObjectMapper().readValue(json, GeoJsonObject.class);
            if (geojson instanceof MultiPolygon) {
                District district = new District();
                district.polygon = (MultiPolygon) geojson;
                district.state = legislator.state;
                district.district = legislator.district;
                return district;
            }

            // otherwise, gracefully choke
            else return null;
        } catch (JsonParseException e) {
            throw new CongressException(e, "Error parsing JSON from " + url);
        } catch (JsonMappingException e) {
            throw new CongressException(e, "Error parsing GeoJSON from " + url);
        } catch (IOException e) { // must go last, catch-all
            throw new CongressException(e, "Error parsing data from " + url);
        }
    }

}