package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import android.widget.Toast;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorList extends ListActivity {
	private final static int LOADING = 0;
	private Legislator[] legislators;
	
	private Button back, refresh;
	
	// whether the user has come to this activity looking to create a shortcut
	private boolean shortcut;
	
	private String zipCode;
	private double latitude = -1;
	private double longitude = -1;
	private String lastName;
	private String state;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.legislator_list);
    	
		Bundle extras = getIntent().getExtras();
		
    	zipCode = extras.getString("zip_code");
    	latitude = extras.getDouble("latitude");
    	longitude = extras.getDouble("longitude");
    	lastName = extras.getString("last_name");
    	state = extras.getString("state");
    	
    	shortcut = extras.getBoolean("shortcut", false);
    	
    	setupControls();
    	loadLegislators();
    }
    
    final Handler handler = new Handler();
    final Runnable updateThread = new Runnable() {
        public void run() {
        	setListAdapter(new ArrayAdapter<Legislator>(LegislatorList.this, android.R.layout.simple_list_item_1, legislators));
        	TextView empty = (TextView) LegislatorList.this.findViewById(R.id.empty_msg);
        	
        	if (legislators.length <= 0) {
        		if (zipSearch())
        			empty.setText(R.string.empty_zipcode);
        		else if (locationSearch())
        			empty.setText(R.string.empty_location);
        		else if (lastNameSearch())
        			empty.setText(R.string.empty_last_name);
        		else if (stateSearch())
        			empty.setText(R.string.empty_state);
        		else
        			empty.setText(R.string.empty_general);
        		back.setVisibility(View.VISIBLE);
        	}
        	
        	removeDialog(LOADING);
        }
    };
    final Runnable updateFailure = new Runnable() {
        public void run() {
        	setListAdapter(new ArrayAdapter<Legislator>(LegislatorList.this, android.R.layout.simple_list_item_1, legislators));
        	TextView empty = (TextView) LegislatorList.this.findViewById(R.id.empty_msg);
        	empty.setText(R.string.connection_failed);
        	refresh.setVisibility(View.VISIBLE);
			removeDialog(LOADING);
        }
    };
    
    public void setupControls() {
    	back = (Button) findViewById(R.id.empty_back);
    	back.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
    	refresh = (Button) findViewById(R.id.empty_refresh);
    	refresh.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loadLegislators();
			}
		});
    }
    
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	Legislator legislator = (Legislator) parent.getItemAtPosition(position);
    	String legislatorId = legislator.getId();
    	Intent legislatorIntent = legislatorIntent(legislatorId);
    	
    	if (shortcut) {
    		// Make sure that shortcuts always open the desired profile, 
    		// instead of bringing someone else's profile to the front who was just open
    		legislatorIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    		
    		BitmapDrawable drawable = LegislatorProfile.getImage(LegislatorProfile.PIC_SMALL, legislatorId);
    		
    		Intent intent = new Intent();
    		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, legislatorIntent);
    		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, legislator.getProperty("lastname"));
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
				
				try {
			    	if (zipSearch())
			    		legislators = Legislator.getLegislatorsForZipCode(api, zipCode);
			    	else if (locationSearch())
			    		legislators = Legislator.getLegislatorsForLatLong(api, latitude, longitude);
			    	else if (lastNameSearch()) {
			    		Map<String,String> params = new HashMap<String,String>();
			    		params.put("lastname", lastName);
			    		legislators = Legislator.allLegislators(api, params);
			    	} else if (stateSearch()) {
			    		Map<String,String> params = new HashMap<String,String>();
			    		params.put("state", state);
			    		legislators = Legislator.allLegislators(api, params);
			    	}
			    	handler.post(updateThread);
				} catch(IOException e) {
					legislators = new Legislator[0];
					handler.post(updateFailure);
				}
	        }
    	};
    	loadingThread.start();
	    
		showDialog(LOADING);
    }
    
    private boolean zipSearch() {
    	return zipCode != null;
    }
    
    private boolean locationSearch() {
    	// sucks for people at the intersection of the equator and prime meridian
    	return (latitude != 0.0 && longitude != 0.0);
    }
    
    private boolean lastNameSearch() {
    	return lastName != null;
    }
    
    private boolean stateSearch() {
    	return state != null;
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