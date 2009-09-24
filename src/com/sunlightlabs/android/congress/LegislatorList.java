package com.sunlightlabs.android.congress;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;

import com.sunlightlabs.android.congress.LegislatorNews.NewsAdapter;
import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorList extends ListActivity {
	private final static int LOADING = 0;
	private Legislator[] legislators;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	loadLegislators();
    }
    
    final Handler handler = new Handler();
    final Runnable updateThread = new Runnable() {
        public void run() {
        	setListAdapter(new ArrayAdapter<Legislator>(LegislatorList.this, android.R.layout.simple_list_item_1, legislators));
        	getListView().setOnItemClickListener(new OnItemClickListener() { 
        		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        			launchLegislator(((Legislator) parent.getItemAtPosition(position)).getId());
        		}
        	});
        	dismissDialog(LOADING);
        }
    };
	
    public void loadLegislators() {
    	Thread loadingThread = new Thread() {
	        public void run() {
		    	String api_key = getResources().getString(R.string.sunlight_api_key);
				ApiCall api = new ApiCall(api_key);
				
				// expand here to handle other types of legislator searches for this list
				Bundle extras = getIntent().getExtras();
				
		    	String zipCode = extras.getString("zip_code");
		    	double latitude = extras.getDouble("latitude");
		    	double longitude = extras.getDouble("longitude");
		    	if (zipCode != null)
		    		legislators = Legislator.getLegislatorsForZipCode(api, zipCode);
		    	else
		    		legislators = Legislator.getLegislatorsForLatLong(api, latitude, longitude);
		    	
		    	handler.post(updateThread);
	        }
    	};
    	loadingThread.start();
	    
		showDialog(LOADING);
    }
    
    public void launchLegislator(String id) {
    	Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorTabs");
		
		Bundle extras = new Bundle();
		extras.putString("legislator_id", id); 
		i.putExtras(extras);
		
		startActivity(i);
    }
    
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOADING:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Finding legislators...");
            return dialog;
        default:
            return null;
        }
    }
}