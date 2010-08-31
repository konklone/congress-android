package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.sunlightlabs.android.congress.notifications.NotificationFinder;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorTabs extends TabActivity {	
	private Legislator legislator;
	private String tab;

	private Database database;
	private Cursor cursor;
	
	private ImageView star;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legislator);

		Bundle extras = getIntent().getExtras();
		legislator = (Legislator) extras.getSerializable("legislator");
		tab = extras.getString("tab");
		
		database = new Database(this);
		database.open();
		cursor = database.getLegislator(legislator.getId());
		startManagingCursor(cursor);

        setupControls();
        setupTabs();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}

	public void setupControls() {
		star = (ImageView) findViewById(R.id.favorite);
		star.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggleDatabaseFavorite();
			}
		});
		
		toggleFavoriteStar(cursor.getCount() == 1);

		TextView nameTitle = (TextView) findViewById(R.id.title_text);
		String titledName = legislator.titledName();
		nameTitle.setText(titledName);
		if (titledName.length() >= 23)
			nameTitle.setTextSize(19);
	}

	private void toggleFavoriteStar(boolean enabled) {
		if (enabled)
			star.setImageResource(R.drawable.star_on);
		else
			star.setImageResource(R.drawable.star_off);
	}

	private void toggleDatabaseFavorite() {
		String id = legislator.getId();
		cursor.requery();
		if (cursor.getCount() == 1) {
			if (database.removeLegislator(id) != 0)
				toggleFavoriteStar(false);
		} else {
			if (database.addLegislator(legislator) != -1) {
				toggleFavoriteStar(true);
				
				if (!Utils.hasShownFavoritesMessage(this)) {
					Utils.alert(this, R.string.legislator_favorites_message);
					Utils.markShownFavoritesMessage(this);
				}
			}
		}
	}

	public void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		
		Utils.addTab(this, tabHost, "profile", profileIntent(), getString(R.string.tab_profile), res.getDrawable(R.drawable.tab_profile));
		Utils.addTab(this, tabHost, "news", newsIntent(), getString(R.string.tab_news), res.getDrawable(R.drawable.tab_news));
		
		String twitter_id = legislator.twitter_id;
		if (legislator.in_office && twitter_id != null && !(twitter_id.equals("")))
			Utils.addTab(this, tabHost, "tweets", twitterIntent(), getString(R.string.tab_tweets), res.getDrawable(R.drawable.tab_twitter));
		
		String youtube_id = legislator.youtubeUsername();
		if (legislator.in_office && youtube_id != null && !(youtube_id.equals("")))
			Utils.addTab(this, tabHost, "videos", youtubeIntent(), getString(R.string.tab_videos), res.getDrawable(R.drawable.tab_video));
		
		if (tab != null) 
			tabHost.setCurrentTabByTag(tab);
		else
			tabHost.setCurrentTabByTag("profile");
	}
	
	public Intent profileIntent() {
		return Utils.legislatorIntent(this, LegislatorProfile.class, legislator);
	}
	
	public Intent newsIntent() {
		return new Intent(this, NewsList.class)
			.putExtra("searchTerm", correctExceptions(searchTermFor(legislator)))
			.putExtra("subscriptionId", legislator.id)
			.putExtra("subscriptionName", NotificationFinder.notificationName(legislator))
			.putExtra("subscriptionClass", "LegislatorNewsFinder");
	}
	
	public Intent twitterIntent() {
		return new Intent(this, LegislatorTwitter.class).putExtra("legislator", legislator);
	}
	
	public Intent youtubeIntent() {
		return new Intent(this, LegislatorYouTube.class).putExtra("legislator", legislator);
	}
	
	
	// for news searching, don't use legislator.titledName() because we don't want to use the name_suffix
	private static String searchTermFor(Legislator legislator) {
    	return legislator.title + ". " + legislator.firstName() + " " + legislator.last_name;
    }
    
	// a little hand massaging for prominent exceptions
    private static String correctExceptions(String name) {
		if (name.equals("Rep. Nancy Pelosi"))
			return "Speaker Nancy Pelosi";
		else if (name.equals("Del. Eleanor Norton"))
			return "Eleanor Holmes Norton";
		else
			return name;
	}

}
