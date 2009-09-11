package com.sunlightlabs.android.congress;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorList extends ListActivity {
	public static String ZIP_CODE = "com.sunlightlabs.android.congress.zip_code";
	private ApiCall api;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	api = new ApiCall("");
    	
    	String zipCode = getIntent().getStringExtra(ZIP_CODE);
    	setListAdapter(new ArrayAdapter<Legislator>(this, android.R.layout.simple_list_item_1, byZip(zipCode)));
    }
    
    public Legislator[] byZip(String zipCode) {
    	return Legislator.getLegislatorsForZipCode(api, zipCode);
    }
    
    public void launchProfile(String id) {
    	Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorProfile");
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		Bundle extras = new Bundle();
		extras.putString(LegislatorProfile.LEGISLATOR_ID, id); 
		i.putExtras(extras);
		
		startActivity(i);
    }
}