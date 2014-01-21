package com.sunlightlabs.android.congress.utils;

import org.osmdroid.util.BoundingBoxE6;

import android.content.Context;
import android.util.AttributeSet;

import com.mapbox.mapboxsdk.MapView;

// overridden to ensure that the bounding box zoom happens post-layout
// http://stackoverflow.com/questions/15658277/zoom-mapview-to-a-certain-bounding-box-on-osmdroid

public class DistrictMapView extends MapView {

	public BoundingBoxE6 boundingBox;
	public boolean zoomedOnce = false;
	
	public DistrictMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public DistrictMapView(Context context, String url) {
		super(context, url);
	}
	
	@Override
	protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
	    super.onLayout(arg0, arg1, arg2, arg3, arg4);

	    if (this.boundingBox != null) {
	        this.zoomToBoundingBox(this.boundingBox);
	        
	        // trigger a zoom level, this helps, but only once
	        if (!zoomedOnce) {
	        	this.getController().zoomIn();
	        	zoomedOnce = true;
	        }
	    }
	}
	
}
