package com.sunlightlabs.android.congress;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorList extends ListActivity {
	private Legislator[] legislators;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	loadLegislators();
    	
    	setListAdapter(new ArrayAdapter<Legislator>(this, android.R.layout.simple_list_item_1, legislators));
    	getListView().setOnItemClickListener(new OnItemClickListener() { 
    		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    			launchLegislator(((Legislator) parent.getItemAtPosition(position)).getId());
    		}
    	});
    }
    
    public void loadLegislators() {
    	String api_key = getResources().getString(R.string.sunlight_api_key);
		ApiCall api = new ApiCall(api_key);
		
		// expand here to handle other types of legislator searches for this list
    	String zipCode = getIntent().getStringExtra("zip_code");
    	legislators = Legislator.getLegislatorsForZipCode(api, zipCode);
    }
    
    public void launchLegislator(String id) {
    	Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorTabs");
		
		Bundle extras = new Bundle();
		extras.putString("legislator_id", id); 
		i.putExtras(extras);
		
		startActivity(i);
    }
}