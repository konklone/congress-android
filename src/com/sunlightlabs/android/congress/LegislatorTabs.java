package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorTabs extends TabActivity {
	private Legislator legislator;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legislator);
        Bundle extras = getIntent().getExtras();
		legislator = (Legislator) extras.getSerializable("legislator");
		
        setupControls();
        setupTabs();
	}
	
	public void setupControls() {
		TextView nameTitle = (TextView) findViewById(R.id.title_text);
		String titledName = legislator.titledName();
		nameTitle.setText(titledName);
		if (titledName.length() >= 23)
			nameTitle.setTextSize(19);
	}
	
	public void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		
		Utils.addTab(this, tabHost, "profile_tab", "Profile", profileIntent(), res.getDrawable(R.drawable.tab_profile));
		Utils.addTab(this, tabHost, "news_tab", "News", newsIntent(), res.getDrawable(R.drawable.tab_news));
		
		String twitter_id = legislator.twitter_id;
		if (twitter_id != null && !(twitter_id.equals("")))
			Utils.addTab(this, tabHost, "twitter_tab", "Twitter", twitterIntent(), res.getDrawable(R.drawable.tab_twitter));
		
		String youtube_id = legislator.youtubeUsername();
		if (youtube_id != null && !(youtube_id.equals("")))
			Utils.addTab(this, tabHost, "youtube_tab", "YouTube", youtubeIntent(), res.getDrawable(R.drawable.tab_video));
			
		tabHost.setCurrentTab(0);
	}
	
	public Intent profileIntent() {
		return Utils.legislatorIntent(this, LegislatorProfile.class, legislator);
	}
	
	public Intent newsIntent() {
		return new Intent(this, LegislatorNews.class)
			.putExtra("firstName", legislator.firstName())
			.putExtra("nickname", legislator.nickname)
			.putExtra("lastName", legislator.last_name)
			.putExtra("title", legislator.title);
	}
	
	public Intent twitterIntent() {
		return new Intent(this, LegislatorTwitter.class)
			.putExtra("username", legislator.twitter_id);
	}
	
	public Intent youtubeIntent() {
		return new Intent(this, LegislatorYouTube.class)
			.putExtra("username", legislator.youtubeUsername());
	}

}
