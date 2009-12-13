package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TextView;

public class LegislatorTabs extends TabActivity {
	private String id, titledName, state, party, 
		gender, domain, office, website, phone, 
		twitter_id, youtube_id;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.legislator);
        
        Bundle extras = getIntent().getExtras();
        id = extras.getString("id");
		titledName = extras.getString("titledName");
		state = extras.getString("state");
		party = extras.getString("party");
		gender = extras.getString("gender");
		domain = extras.getString("domain");
		office = extras.getString("office");
		website = extras.getString("website");
		phone = extras.getString("phone");
		twitter_id = extras.getString("twitter_id");
		youtube_id = extras.getString("youtube_id");
		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        ((TextView) findViewById(R.id.custom_title)).setText(titledName);
    		
        setupTabs();
	}
	
	public void setupTabs() {
		Resources res = this.getResources();
		TabHost tabHost = getTabHost();
		
		tabHost.addTab(tabHost.newTabSpec("profile_tab").setIndicator("Profile", res.getDrawable(R.drawable.tab_profile)).setContent(profileIntent()));
		tabHost.addTab(tabHost.newTabSpec("news_tab").setIndicator("News", res.getDrawable(R.drawable.tab_news)).setContent(newsIntent()));
		
		if (!(twitter_id.equals("")))
			tabHost.addTab(tabHost.newTabSpec("twitter_tab").setIndicator("Twitter", res.getDrawable(R.drawable.tab_twitter)).setContent(twitterIntent()));
		
		if (!(youtube_id.equals("")))
			tabHost.addTab(tabHost.newTabSpec("youtube_tab").setIndicator("YouTube", res.getDrawable(R.drawable.tab_video)).setContent(youtubeIntent()));
		
		tabHost.setCurrentTab(0);
	}
	
	public Intent profileIntent() {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorProfile");
		
		Bundle extras = new Bundle();
		extras.putString("id", id);
		extras.putString("titledName", titledName);
		extras.putString("state", state);
		extras.putString("party", party);
		extras.putString("gender", gender);
		extras.putString("domain", domain);
		extras.putString("office", office);
		extras.putString("website", website);
		extras.putString("phone", phone);
		
		intent.putExtras(extras);
		return intent;
	}
	
	public Intent newsIntent() {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorNews");
		
		Bundle extras = new Bundle();
		extras.putString("searchName", titledName);
		
		intent.putExtras(extras);
		return intent;
	}
	
	public Intent twitterIntent() {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorTwitter");
		
		Bundle extras = new Bundle();
		extras.putString("username", twitter_id);
		
		intent.putExtras(extras);
		return intent;
	}
	
	public Intent youtubeIntent() {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorYouTube");
		
		Bundle extras = new Bundle();
		extras.putString("username", youtube_id);
		
		intent.putExtras(extras);
		return intent;
	}

}
