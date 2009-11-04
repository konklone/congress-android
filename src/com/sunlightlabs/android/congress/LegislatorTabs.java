package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorTabs extends TabActivity {
	private static final int LOADING = 0;
	
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
        
        legislator = ((Legislator) getLastNonConfigurationInstance());
        
        loadLegislator(id);
	}
	
	@Override
    public Object onRetainNonConfigurationInstance() {
    	return legislator;
    }
	
	final Handler handler = new Handler();
	final Runnable loadingSuccess = new Runnable() {
		public void run() {
			displayLegislator();
			removeDialog(LOADING);
		}
	};
	
	final Runnable loadingFailure = new Runnable() {
		public void run() {
			alert("Couldn't connect to the network. Please try again when you have a connection.");
			removeDialog(LOADING);
			finish();
		}
	};
	
	public void displayLegislator() {
		customTitle.setText(legislator.titledName());
		setupTabs();
	}
	
	public void loadLegislator(String legId) {
		final String id = legId;
		Thread loadingThread = new Thread() {
			public void run() {
				try {
					legislator = Legislator.getLegislatorById(new ApiCall(apiKey), id);
					handler.post(loadingSuccess);
				} catch(IOException e) {
					handler.post(loadingFailure);
				}
			}
		};
		
		if (legislator == null) {
			loadingThread.start();
			showDialog(LOADING);
		} else {
			displayLegislator();
		}
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
	
	protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOADING:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Loading...");
            dialog.setCancelable(false);
            return dialog;
        default:
            return null;
        }
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

}
