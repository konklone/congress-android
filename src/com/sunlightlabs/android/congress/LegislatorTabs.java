package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorTabs extends TabActivity {
	private TabHost tabHost;
	private Legislator legislator;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legislator);
        
        String id = getIntent().getStringExtra("legislator_id");
        loadLegislator(id);
        
        setupTabs();
	}
	
	public void loadLegislator(String id) {
		String api_key = getResources().getString(R.string.sunlight_api_key);
		ApiCall api = new ApiCall(api_key);
		legislator = Legislator.getLegislatorById(api, id);
	}
	
	public void setupTabs() {
		tabHost = getTabHost();
		
		tabHost.addTab(tabHost.newTabSpec("profile_tab").setIndicator("Profile").setContent(profileIntent()));
		tabHost.addTab(tabHost.newTabSpec("news_tab").setIndicator("News").setContent(newsIntent()));
		
		if (!legislator.getProperty("twitter_id").equals(""))
			tabHost.addTab(tabHost.newTabSpec("twitter_tab").setIndicator("Twitter").setContent(twitterIntent()));
		
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
		extras.putString("youtube_url", legislator.getProperty("youtube_url"));
		
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

}
