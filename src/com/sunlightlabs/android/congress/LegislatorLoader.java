package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.LegislatorService;

public class LegislatorLoader extends Activity {
	private LoadLegislatorTask loadLegislatorTask = null;
	private String id;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading_fullscreen);
		
		id = getIntent().getStringExtra("legislator_id");
        
        loadLegislatorTask = (LoadLegislatorTask) getLastNonConfigurationInstance();
        if (loadLegislatorTask != null)
        	loadLegislatorTask.onScreenLoad(this);
        else
        	loadLegislatorTask = (LoadLegislatorTask) new LoadLegislatorTask(this).execute(id);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadLegislatorTask;
	}
	
	
	public void onLoadLegislator(Legislator legislator) {
		if (legislator != null)
			startActivity(Utils.legislatorIntent(this, legislator));
		else
			Utils.alert(this, R.string.error_connection);
		
		finish();
	}
	
	private class LoadLegislatorTask extends AsyncTask<String,Void,Legislator> {
		public LegislatorLoader context;
    	
    	public LoadLegislatorTask(LegislatorLoader context) {
    		super();
    		this.context = context;
    		Utils.setupSunlight(context);
    	}
    	
    	public void onScreenLoad(LegislatorLoader context) {
    		this.context = context;
    	}
    	
    	@Override
    	protected Legislator doInBackground(String... id) {
    		try {
				return LegislatorService.find(id[0]);
			} catch(CongressException exception) {
				return null;
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Legislator legislator) {
    		if (isCancelled()) return;
    		context.loadLegislatorTask = null;
    		
    		context.onLoadLegislator(legislator);
    	}
    }
}
