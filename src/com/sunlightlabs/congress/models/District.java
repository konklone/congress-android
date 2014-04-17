package com.sunlightlabs.congress.models;

import android.graphics.Color;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.sunlightlabs.android.congress.utils.Utils;

import org.geojson.LngLatAlt;
import org.geojson.MultiPolygon;

import java.util.List;

// GeoJSON-based map from the web
public class District {

    // parent geoJSON type, for aid in drawing
    public MultiPolygon polygon;

    // record the state and district value, same as legislator values
    public String state, district;

    // helper function to draw a MultiPolygon onto a MapView
    // this is tied to the MapBox Android SDK - this could be moved into a Utils package for better decoupling.
    public static BoundingBox drawDistrict(District district, MapView map) {
        MultiPolygon multiPolygon = district.polygon;
        List<List<List<LngLatAlt>>> polygons = multiPolygon.getCoordinates();
        int numPolygons = polygons.size();

        // min value is, stupidly, 0 (http://stackoverflow.com/questions/3884793/minimum-values-and-double-min-value-in-java)
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (int i=0; i<numPolygons; i++) {
            List<List<LngLatAlt>> rings = polygons.get(i);

            int numRings = rings.size();
            for (int j=0; j<numRings; j++) {
                List<LngLatAlt> points = rings.get(0);
                PathOverlay line = new PathOverlay(Color.BLUE, 2);

                int numPoints = points.size();
                for (int k=0; k<numPoints; k++) {
                    LngLatAlt point = points.get(k);
                    double x = point.getLongitude();
                    double y = point.getLatitude();
                    line.addPoint(new LatLng(y, x));

                    // establish bounding box
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }

                map.getOverlays().add(line);
            }
        }

        // send in a flip if the shape crosses the line opposing the prime meridian
        // (in other words, only Alaska)
        if (district.state.equals("AK")) {
            double left = maxX;
            double right = minX;
            minX = left;
            maxX = right;
        }

        // north, east, south, west
        double s = 0.5; // padding, in degrees

        BoundingBox box = new BoundingBox(maxY+s, maxX+s, minY-s, minX-s);
        Log.i(Utils.TAG, "Bounding box: " + box.getLatNorth() + "|" + box.getLonEast() + "|" + box.getLatSouth() + "|" + box.getLonWest());
        return box;
    }
}