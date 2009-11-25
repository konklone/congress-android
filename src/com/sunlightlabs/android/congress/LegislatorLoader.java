package com.sunlightlabs.android.congress;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorLoader extends Activity {
	private ProgressDialog dialog = null;
	private LoadLegislatorTask loadLegislatorTask = null;
	
	private String apiKey;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String legislator_id = getIntent().getStringExtra("legislator_id");
        apiKey = getResources().getString(R.string.sunlight_api_key);
        
        loadLegislatorTask = (LoadLegislatorTask) getLastNonConfigurationInstance();
        if (loadLegislatorTask != null) {
        	loadLegislatorTask.context = this;
        	loadingDialog();
        } else
        	new LoadLegislatorTask(this).execute(legislator_id);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadLegislatorTask;
	}
	
	public void launchLegislator(Legislator legislator) {
		Intent intent = new Intent();
		
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorTabs");
		
		Bundle extras = new Bundle();
		
		extras.putString("id", legislator.getId());
		extras.putString("titledName", legislator.titledName());
		extras.putString("state", legislator.getProperty("state"));
		extras.putString("party", legislator.getProperty("party"));
		extras.putString("gender", legislator.getProperty("gender"));
		extras.putString("domain", legislator.getDomain());
		extras.putString("office", legislator.getProperty("congress_office"));
		extras.putString("website", legislator.getProperty("website"));
		extras.putString("phone", legislator.getProperty("phone"));
		extras.putString("twitter_id", legislator.getProperty("twitter_id"));
		extras.putString("youtube_id", legislator.youtubeUsername());
		
		intent.putExtras(extras);
		
		startActivity(intent);
		finish();
	}
	
	@Override
    public void onSaveInstanceState(Bundle state) {
    	
    	super.onSaveInstanceState(state);
    }
	
	public void loadingDialog() {
		dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Loading legislator...");
        dialog.setCancelable(false);
		dialog.show();
	}
	
	public void alert(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
	private class LoadLegislatorTask extends AsyncTask<String,Void,Legislator> {
		public LegislatorLoader context;
    	
    	public LoadLegislatorTask(LegislatorLoader context) {
    		super();
    		
    		// link the task and the context
    		this.context = context;
    		this.context.loadLegislatorTask = this;
    	}
		
    	@Override
    	protected void onPreExecute() {
    		context.loadingDialog();
    	}
    	
    	@Override
    	protected Legislator doInBackground(String... ids) {
    		try {
				return Legislator.getLegislatorById(new ApiCall(apiKey), ids[0]);
			} catch(IOException e) {
				return null;
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Legislator legislator) {
    		if (context.dialog != null && context.dialog.isShowing())
        		context.dialog.dismiss();
    		
    		if (legislator != null) {
    			context.launchLegislator(legislator);
    		} else {
    			context.alert("Couldn't connect to the network. Please try again when you have a connection.");
    			context.finish();
    		}
    		context.loadLegislatorTask = null;
    	}
    }
}
