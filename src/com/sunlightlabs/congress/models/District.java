package com.sunlightlabs.congress.models;

import org.geojson.MultiPolygon;

import com.mapbox.mapboxsdk.MapView;

// GeoJSON-based map from the web
public class District {
	
	// parent geoJSON type, for aid in drawing
	public MultiPolygon polygon;
	
	// helper function to draw a MultiPolygon onto a MapView
	// this is tied to the MapBox Android SDK - this could be moved into a Utils package for better decoupling.
	public static void drawPolygon(MultiPolygon polygon, MapView map) {
		
	}
}