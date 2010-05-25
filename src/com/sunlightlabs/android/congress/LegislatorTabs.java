package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;

public class LegislatorTabs extends TabActivity {
	private String id, state, party,
		titledName, lastName, firstName, nickname, nameSuffix, title, 
		gender, domain, office, website, phone, 
		twitter_id, youtube_id, bioguide_id, govtrack_id;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legislator);
        Bundle extras = getIntent().getExtras();
        id = extras.getString("id");
		titledName = extras.getString("titledName");
		lastName = extras.getString("lastName");
		firstName = extras.getString("firstName");
		nickname = extras.getString("nickname");
		nameSuffix = extras.getString("nameSuffix");
		title = extras.getString("title");
		state = extras.getString("state");
		party = extras.getString("party");
		gender = extras.getString("gender");
		domain = extras.getString("domain");
		office = extras.getString("office");
		website = extras.getString("website");
		phone = extras.getString("phone");
		twitter_id = extras.getString("twitter_id");
		youtube_id = extras.getString("youtube_id");
		bioguide_id = extras.getString("bioguide_id");
		govtrack_id = extras.getString("govtrack_id");
		
        setupControls();
        setupTabs();
	}
	
	public void setupControls() {
		TextView nameTitle = (TextView) findViewById(R.id.title_text);
		nameTitle.setText(titledName);
		if (titledName.length() >= 23)
			nameTitle.setTextSize(19);
	}
	
	public void setupTabs() {
		Resources res = this.getResources();
		TabHost tabHost = getTabHost();
		
		tabHost.addTab(tabHost.newTabSpec("profile_tab").setIndicator("Profile", res.getDrawable(R.drawable.tab_profile)).setContent(profileIntent()));
		tabHost.addTab(tabHost.newTabSpec("news_tab").setIndicator("News", res.getDrawable(R.drawable.tab_news)).setContent(newsIntent()));
		
		if (twitter_id != null && !(twitter_id.equals("")))
			tabHost.addTab(tabHost.newTabSpec("twitter_tab").setIndicator("Twitter", res.getDrawable(R.drawable.tab_twitter)).setContent(twitterIntent()));
		
		if (youtube_id != null && !(youtube_id.equals("")))
			tabHost.addTab(tabHost.newTabSpec("youtube_tab").setIndicator("YouTube", res.getDrawable(R.drawable.tab_video)).setContent(youtubeIntent()));
		
		tabHost.setCurrentTab(0);
	}
	
	public Intent profileIntent() {
		return new Intent(this, LegislatorInfo.class)
			.putExtra("id", id)
			.putExtra("titledName", titledName)
			.putExtra("lastName", lastName)
			.putExtra("state", state)
			.putExtra("party", party)
			.putExtra("gender", gender)
			.putExtra("domain", domain)
			.putExtra("office", office)
			.putExtra("website", website)
			.putExtra("phone", phone)
			.putExtra("bioguide_id", bioguide_id)
			.putExtra("govtrack_id", govtrack_id);
	}
	
	public Intent newsIntent() {
		return new Intent(this, LegislatorNews.class)
			.putExtra("firstName", firstName)
			.putExtra("nickname", nickname)
			.putExtra("lastName", lastName)
			.putExtra("title", title);
	}
	
	public Intent twitterIntent() {
		return new Intent(this, LegislatorTwitter.class)
			.putExtra("username", twitter_id);
	}
	
	public Intent youtubeIntent() {
		return new Intent(this, LegislatorYouTube.class)
			.putExtra("username", youtube_id);
	}

}
