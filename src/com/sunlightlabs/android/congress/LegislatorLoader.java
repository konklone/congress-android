package com.sunlightlabs.android.congress;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorLoader extends Activity {
	private LoadLegislatorTask loadLegislatorTask = null;
	
	private String apiKey;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String legislator_id = getIntent().getStringExtra("legislator_id");
        apiKey = getResources().getString(R.string.sunlight_api_key);
        
        loadLegislatorTask = (LoadLegislatorTask) getLastNonConfigurationInstance();
        if (loadLegislatorTask != null)
        	loadLegislatorTask.onScreenLoad(this);
        else
        	loadLegislatorTask = (LoadLegislatorTask) new LoadLegislatorTask(this).execute(legislator_id);
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
		extras.putString("lastName", legislator.getProperty("lastname"));
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
	
	public void alert(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
	private class LoadLegislatorTask extends AsyncTask<String,Void,Legislator> {
		public LegislatorLoader context;
		private ProgressDialog dialog = null;
    	
    	public LoadLegislatorTask(LegislatorLoader context) {
    		super();
    		this.context = context;
    	}
		
    	@Override
    	protected void onPreExecute() {
    		loadingDialog();
    	}
    	
    	public void onScreenLoad(LegislatorLoader context) {
    		this.context = context;
    		loadingDialog();
    	}
    	
    	@Override
    	protected Legislator doInBackground(String... ids) {
    		try {
				return Legislator.getLegislatorById(new ApiCall(context.apiKey), ids[0]);
			} catch(IOException e) {
				return null;
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Legislator legislator) {
    		if (dialog != null && dialog.isShowing())
        		dialog.dismiss();
    		
    		if (legislator != null)
    			context.launchLegislator(legislator);
    		else {
    			Utils.alert(LegislatorLoader.this, R.string.error_connection);
    			context.finish();
    		}
    		context.loadLegislatorTask = null;
    	}
    	
    	private void loadingDialog() {
    		dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Loading legislator...");
            
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cancel(true);
					context.finish();
				}
			});
            
    		dialog.show();
    	}
    }
}
