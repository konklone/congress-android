package com.sunlightlabs.android.congress;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TabHost;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorTabs extends TabActivity {
	private static final int LOADING = 0;
	
	private Legislator legislator;
	private String apiKey;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legislator);
        
        String id = getIntent().getStringExtra("legislator_id");
        apiKey = getResources().getString(R.string.sunlight_api_key);
        
        loadLegislator(id);
	}
	
	final Handler handler = new Handler();
	final Runnable doneLoading = new Runnable() {
		public void run() {
			setupTabs();
			
			removeDialog(LOADING);
		}
	};
	
	public void loadLegislator(String legId) {
		final String id = legId;
		Thread loadingThread = new Thread() {
			public void run() {
				legislator = Legislator.getLegislatorById(new ApiCall(apiKey), id);
				handler.post(doneLoading);
			}
		};
		loadingThread.start();
		
		showDialog(LOADING);
	}
	
	
	public void setupTabs() {
		TabHost tabHost = getTabHost();
		
		Intent profileIntent = profileIntent();
		tabHost.addTab(tabHost.newTabSpec("profile_tab").setIndicator("Profile").setContent(profileIntent));
		tabHost.addTab(tabHost.newTabSpec("news_tab").setIndicator("News").setContent(newsIntent()));
		
		if (!legislator.getProperty("twitter_id").equals(""))
			tabHost.addTab(tabHost.newTabSpec("twitter_tab").setIndicator("Twitter").setContent(twitterIntent()));
		
		if (!youtubeUsername(legislator).equals(""))
			tabHost.addTab(tabHost.newTabSpec("youtube_tab").setIndicator("YouTube").setContent(youtubeIntent()));
		
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
			String username = m.group(1);
			int x = 1;
			return username;
		} else
			return "";
	}

}
