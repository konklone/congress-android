package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;

public class LegislatorTabs extends TabActivity {
	private String id, state, party,
		titledName, lastName, firstName, nickname, title, 
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
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		
		Utils.addTab(this, tabHost, "profile_tab", "Profile", profileIntent(), res.getDrawable(R.drawable.tab_profile));
		Utils.addTab(this, tabHost, "news_tab", "News", newsIntent(), res.getDrawable(R.drawable.tab_news));
		
		if (twitter_id != null && !(twitter_id.equals("")))
			Utils.addTab(this, tabHost, "twitter_tab", "Twitter", twitterIntent(), res.getDrawable(R.drawable.tab_twitter));
		
		if (youtube_id != null && !(youtube_id.equals("")))
			Utils.addTab(this, tabHost, "youtube_tab", "YouTube", youtubeIntent(), res.getDrawable(R.drawable.tab_video));
			
		tabHost.setCurrentTab(0);
	}
	
	public Intent profileIntent() {
		return new Intent(this, LegislatorProfile.class)
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
