package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.sunlightlabs.android.congress.utils.LegislatorAdapter;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Committee;
import com.sunlightlabs.entities.Legislator;

public class LegislatorList extends ListActivity {
	private final static int SEARCH_ZIP = 0;
	private final static int SEARCH_LOCATION = 1;
	private final static int SEARCH_STATE = 2;
	private final static int SEARCH_LASTNAME = 3;
	private final static int SEARCH_COMMITTEE = 4;
	
	private ArrayList<Legislator> legislators = null;
	private LoadLegislatorsTask loadLegislatorsTask = null;
	private ShortcutImageTask shortcutImageTask = null;
	private Button back, refresh;
	
	private boolean shortcut;
	
	private String zipCode, lastName, state, api_key, committeeId, committeeName;
	private double latitude = -1;
	private double longitude = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.list);
    	
		Bundle extras = getIntent().getExtras();
		
    	zipCode = extras.getString("zip_code");
    	latitude = extras.getDouble("latitude");
    	longitude = extras.getDouble("longitude");
    	lastName = extras.getString("last_name");
    	state = extras.getString("state");
    	committeeId = extras.getString("committeeId");
    	committeeName = extras.getString("committeeName");

    	api_key = getResources().getString(R.string.sunlight_api_key);
    	
    	shortcut = extras.getBoolean("shortcut", false);
    	
    	setupControls();    	
    	
    	LegislatorListHolder holder = (LegislatorListHolder) getLastNonConfigurationInstance();
    	if (holder != null) {
    		legislators = holder.legislators;
    		loadLegislatorsTask = holder.loadLegislatorsTask;
    		shortcutImageTask = holder.shortcutImageTask;
    	}
    	
    	if (loadLegislatorsTask == null && shortcutImageTask == null)
			loadLegislators();
    	else {
    		if (loadLegislatorsTask != null)
    			loadLegislatorsTask.onScreenLoad(this);
    		
    		if (shortcutImageTask != null)
    			shortcutImageTask.onScreenLoad(this);
    	}
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	LegislatorListHolder holder = new LegislatorListHolder();
    	holder.legislators = this.legislators;
    	holder.loadLegislatorsTask = this.loadLegislatorsTask;
    	holder.shortcutImageTask = this.shortcutImageTask;
    	return holder;
    }
    
    public void loadLegislators() {
    	if (legislators == null)
    		loadLegislatorsTask = (LoadLegislatorsTask) new LoadLegislatorsTask(this).execute();
    	else
    		displayLegislators();
    }
    
    public void displayLegislators() {
    	if (legislators.size() > 0)
    		setListAdapter(new LegislatorAdapter(this, legislators));
    	else {
    		switch (searchType()) {
	    		case SEARCH_ZIP:
	    			Utils.showBack(this, R.string.empty_zipcode);
	    			break;
	    		case SEARCH_LOCATION:
	    			Utils.showBack(this, R.string.empty_location);
	    			break;
	    		case SEARCH_LASTNAME:
	    			Utils.showBack(this, R.string.empty_last_name);
	    			break;
	    		default:
	    			Utils.showBack(this, R.string.empty_general);
    		}
    	}
    }
    
    public void setupControls() {
    	back = (Button) findViewById(R.id.back);
    	back.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
    	refresh = (Button) findViewById(R.id.refresh);
    	refresh.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Utils.showLoading(LegislatorList.this);
				loadLegislators();
			}
		});
    	
    	switch(searchType()) {
    	case SEARCH_ZIP:
    		setTitle("Legislators for " + zipCode + ":");
    		break;
		case SEARCH_LOCATION:
    		setTitle("Legislators for your location:");
    		break;
		case SEARCH_LASTNAME:
    		setTitle("Legislators by the name of " + lastName + ":");
    		break;
		case SEARCH_COMMITTEE:
			setTitle(committeeName);
			break;
		case SEARCH_STATE:
    		setTitle("Legislators from " + Utils.stateCodeToName(this, state) + ":");
    		break;
    	default:
    		setTitle("Legislator search:");
    	}
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	selectLegislator((Legislator) parent.getItemAtPosition(position));
    }
    
    public void selectLegislator(Legislator legislator) {
    	if (shortcut)
    		shortcutImageTask = (ShortcutImageTask) new ShortcutImageTask(this, legislator).execute();
    	else {
    		String legislatorId = legislator.getId();
        	Intent legislatorIntent = Utils.legislatorIntent(legislatorId);
    		startActivity(legislatorIntent);
    	}
    }
    
    private int searchType() {
    	if (zipSearch())
    		return SEARCH_ZIP;
    	else if (locationSearch())
    		return SEARCH_LOCATION;
    	else if (lastNameSearch())
    		return SEARCH_LASTNAME;
    	else if (stateSearch())
    		return SEARCH_STATE;
    	else if (committeeSearch())
    		return SEARCH_COMMITTEE;
    	else
    		return SEARCH_LOCATION;
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
    
    private boolean committeeSearch() {
    	return committeeId != null;
    }
    
    public void returnShortcutIcon(Legislator legislator, Bitmap icon) {
    	setResult(RESULT_OK, Utils.shortcutIntent(this, legislator, icon));
		finish();
    }
    
    
    private class ShortcutImageTask extends AsyncTask<Void,Void,Bitmap> {
    	public LegislatorList context;
    	public Legislator legislator;
    	private ProgressDialog dialog;
    	
    	public ShortcutImageTask(LegislatorList context, Legislator legislator) {
    		super();
    		this.legislator = legislator;
    		this.context = context;
    		this.context.shortcutImageTask = this;
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		loadingDialog();
    	}
    	
    	public void onScreenLoad(LegislatorList context) {
    		this.context = context;
    		loadingDialog();
    	}
    	
    	@Override
    	protected Bitmap doInBackground(Void... nothing) {
    		return LegislatorImage.shortcutImage(legislator.getId(), context);
    	}
    	
    	@Override
    	protected void onPostExecute(Bitmap shortcutIcon) {
    		if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
    		
    		context.returnShortcutIcon(legislator, shortcutIcon);
    		
    		context.shortcutImageTask = null;
    	}
    	
    	public void loadingDialog() {
        	dialog = new ProgressDialog(context);
        	dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        	dialog.setMessage("Creating shortcut...");
        	
        	dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cancel(true);
    				context.finish();
				}
			});
        	
        	dialog.show();
        }
    }
    
    private class LoadLegislatorsTask extends AsyncTask<Void,Void,ArrayList<Legislator>> {
    	public LegislatorList context;
    	private ProgressDialog dialog;
    	
    	public LoadLegislatorsTask(LegislatorList context) {
    		super();
    		this.context = context;
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		loadingDialog();
    	}
    	
    	public void onScreenLoad(LegislatorList context) {
    		this.context = context;
    		loadingDialog();
    	}
    	
    	@Override
    	protected ArrayList<Legislator> doInBackground(Void... nothing) {
    		ApiCall api = new ApiCall(api_key);
			
    		ArrayList<Legislator> legislators = new ArrayList<Legislator>();
    		ArrayList<Legislator> lower = new ArrayList<Legislator>(); // lower house, not stature
    		Legislator[] temp;
			try {
				Map<String,String> params;
				switch (searchType()) {
					case SEARCH_ZIP:
			    		temp = Legislator.getLegislatorsForZipCode(api, zipCode);
			    		break;
					case SEARCH_LOCATION:
						temp = Legislator.getLegislatorsForLatLong(api, latitude, longitude);
			    		break;
					case SEARCH_LASTNAME:
			    		params = new HashMap<String,String>();
			    		params.put("lastname", lastName);
			    		temp = Legislator.allLegislators(api, params);
			    		break;
					case SEARCH_COMMITTEE:
						temp = Committee.getLegislatorsForCommittee(api, committeeId);
						break;
					case SEARCH_STATE:
			    		params = new HashMap<String,String>();
			    		params.put("state", state);
			    		temp = Legislator.allLegislators(api, params);
			    		break;
			    	default:
			    		return legislators;
				}
				
				// sort legislators Senators-first
				for (int i=0; i<temp.length; i++) {
					if (temp[i].getProperty("title").equals("Sen"))
						legislators.add(temp[i]);
					else
						lower.add(temp[i]);
				}
				Collections.sort(legislators);
				Collections.sort(lower);
				legislators.addAll(lower);
				
				return legislators;
		    	
			} catch(IOException e) {
				return legislators;
			}
    	}
    	
    	@Override
    	protected void onPostExecute(ArrayList<Legislator> legislators) {
    		if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
    		
    		context.legislators = legislators;
    		
			// if there's only one result, don't even make them click it
        	if (legislators.size() == 1) {
        		context.selectLegislator(legislators.get(0));
        		
        		// if we're going on to the profile of a legislator, we want to cut the list out of the stack
        		// but if we're generating a shortcut, the shortcut process will be spawning off 
        		// a separate background thread, that needs a live activity while it works, 
        		// and will call finish() on its own
        		if (!shortcut) 
        			context.finish();
        	} else
        		context.displayLegislators();
        	
    		context.loadLegislatorsTask = null;
    	}
    	
    	public void loadingDialog() {
        	dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Finding legislators...");
            
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cancel(true);
    				context.finish();
				}
			});
            
            dialog.show();
        }
    }
    
    static class LegislatorListHolder {
    	ArrayList<Legislator> legislators;
    	LoadLegislatorsTask loadLegislatorsTask;
    	ShortcutImageTask shortcutImageTask;
    }
}