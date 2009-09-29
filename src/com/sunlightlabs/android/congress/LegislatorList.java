package com.sunlightlabs.android.congress;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorList extends ListActivity {
	private final static int LOADING = 0;
	private Legislator[] legislators;
	
	private Button back;
	
	// whether the user has come to this activity looking to create a shortcut
	private boolean shortcut;
	
	private String zipCode;
	private double latitude = -1;
	private double longitude = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.legislator_list);
    	
		Bundle extras = getIntent().getExtras();
		
    	zipCode = extras.getString("zip_code");
    	latitude = extras.getDouble("latitude");
    	longitude = extras.getDouble("longitude");
    	
    	shortcut = extras.getBoolean("shortcut", false);
    	
    	setupControls();
    	loadLegislators();
    }
    
    final Handler handler = new Handler();
    final Runnable updateThread = new Runnable() {
        public void run() {
        	setListAdapter(new ArrayAdapter<Legislator>(LegislatorList.this, android.R.layout.simple_list_item_1, legislators));
        	
        	TextView empty = (TextView) LegislatorList.this.findViewById(R.id.empty_msg);
        	int length = legislators.length;
        	if (legislators.length <= 0) {
        		if (zipSearch())
        			empty.setText(R.string.empty_zipcode);
        		else if (locationSearch())
        			empty.setText(R.string.empty_location);
        		else
        			empty.setText(R.string.empty_general);
        		back.setVisibility(View.VISIBLE);
        	}
        	
        	dismissDialog(LOADING);
        }
    };
    
    public void setupControls() {
    	back = (Button) LegislatorList.this.findViewById(R.id.empty_back);
    	back.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
    }
    
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	Legislator legislator = (Legislator) parent.getItemAtPosition(position);
    	String legislatorId = legislator.getId();
    	Intent legislatorIntent = legislatorIntent(legislatorId);
    	
    	if (shortcut) {
    		String name = legislator.getProperty("title") + ". " + legislator.getProperty("lastname");
    		
    		BitmapDrawable drawable = LegislatorProfile.getImage(LegislatorProfile.PIC_SMALL, legislatorId);
    		
    		Intent intent = new Intent();
    		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, legislatorIntent);
    		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
    		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, drawable.getBitmap());
    		
    		setResult(RESULT_OK, intent);
    		finish();
    	} else
    		startActivity(legislatorIntent);
    }
	
    public void loadLegislators() {
    	Thread loadingThread = new Thread() {
	        public void run() {
		    	String api_key = getResources().getString(R.string.sunlight_api_key);
				ApiCall api = new ApiCall(api_key);
				
		    	if (zipSearch())
		    		legislators = Legislator.getLegislatorsForZipCode(api, zipCode);
		    	else if (locationSearch())
		    		legislators = Legislator.getLegislatorsForLatLong(api, latitude, longitude);
		    	
		    	handler.post(updateThread);
	        }
    	};
    	loadingThread.start();
	    
		showDialog(LOADING);
    }
    
    private boolean zipSearch() {
    	return zipCode != null;
    }
    
    private boolean locationSearch() {
    	// sucks for people at the equator
    	return (latitude != 0.0 && longitude != 0.0);
    }
    
    public void launchLegislator(String id) {
		startActivity(legislatorIntent(id));
    }
    
    public Intent legislatorIntent(String id) {
    	Intent i = new Intent(Intent.ACTION_MAIN);
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorTabs");
		
		Bundle extras = new Bundle();
		extras.putString("legislator_id", id); 
		i.putExtras(extras);
		
		return i;
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