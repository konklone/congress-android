package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorTabs extends TabActivity {
	private ProgressDialog dialog = null;
	
	private Legislator legislator;
	private String apiKey;
	private TextView customTitle;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.legislator);
        
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        customTitle = (TextView) findViewById(R.id.custom_title);
        
        String id = getIntent().getStringExtra("legislator_id");
        apiKey = getResources().getString(R.string.sunlight_api_key);
        
        loadLegislator(id);
	}
	
	@Override
    public Object onRetainNonConfigurationInstance() {
    	return legislator;
    }
	
	@Override
    public void onSaveInstanceState(Bundle state) {
    	if (dialog != null && dialog.isShowing())
    		dialog.dismiss();
    	super.onSaveInstanceState(state);
    }
	
	public void loadLegislator(String id) {
		legislator = (Legislator) getLastNonConfigurationInstance();
		if (legislator == null)
        	new LoadLegislator().execute(id);
        else
        	displayLegislator();
	}
	
	public void displayLegislator() {
		customTitle.setText(legislator.titledName());
		setupTabs();
	}	
	
	public void setupTabs() {
		TabHost tabHost = getTabHost();
		
		Resources res = this.getResources();
		
		Intent profileIntent = profileIntent();
		tabHost.addTab(tabHost.newTabSpec("profile_tab").setIndicator("Profile", res.getDrawable(R.drawable.tab_profile)).setContent(profileIntent));
		tabHost.addTab(tabHost.newTabSpec("news_tab").setIndicator("News", res.getDrawable(R.drawable.tab_news)).setContent(newsIntent()));
		
		if (!legislator.getProperty("twitter_id").equals(""))
			tabHost.addTab(tabHost.newTabSpec("twitter_tab").setIndicator("Twitter", res.getDrawable(R.drawable.tab_twitter)).setContent(twitterIntent()));
		
		if (!youtubeUsername(legislator).equals(""))
			tabHost.addTab(tabHost.newTabSpec("youtube_tab").setIndicator("YouTube", res.getDrawable(R.drawable.tab_video)).setContent(youtubeIntent()));
		
		tabHost.setCurrentTab(0);
	}
	
	public Intent profileIntent() {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorProfile");
		
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
		extras.putString("youtube_id", youtubeUsername(legislator));
		
		intent.putExtras(extras);
		return intent;
	}
	
	public Intent newsIntent() {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorNews");
		
		Bundle extras = new Bundle();
		extras.putString("searchName", legislator.titledName());
		
		intent.putExtras(extras);
		return intent;
	}
	
	public Intent twitterIntent() {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorTwitter");
		
		Bundle extras = new Bundle();
		extras.putString("username", legislator.getProperty("twitter_id"));
		
		intent.putExtras(extras);
		return intent;
	}
	
	public Intent youtubeIntent() {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorYouTube");
		
		Bundle extras = new Bundle();
		extras.putString("username", youtubeUsername(legislator));
		
		intent.putExtras(extras);
		return intent;
	}
	
	public static String youtubeUsername(Legislator legislator) {
		String url = legislator.getProperty("youtube_url");
		Pattern p = Pattern.compile("http://(?:www\\.)?youtube\\.com/(?:user/)?(.*?)/?$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(url);
		boolean found = m.find();
		if (found) {
			return m.group(1);
		} else
			return "";
	}
	
	public void alert(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
	private class LoadLegislator extends AsyncTask<String,Void,Boolean> {
    	@Override
    	protected void onPreExecute() {
    		dialog = new ProgressDialog(LegislatorTabs.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Loading legislator...");
            dialog.setCancelable(false);
			dialog.show();
    	}
    	
    	@Override
    	protected Boolean doInBackground(String... ids) {
    		try {
				legislator = Legislator.getLegislatorById(new ApiCall(apiKey), ids[0]);
				return new Boolean(true);
			} catch(IOException e) {
				return new Boolean(false);
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Boolean result) {
    		dialog.dismiss();
    		if (result.booleanValue()) {
    			displayLegislator();
    		} else {
    			alert("Couldn't connect to the network. Please try again when you have a connection.");
    			finish();
    		}
    	}
    }

}
