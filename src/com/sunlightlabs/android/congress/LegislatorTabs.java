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

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorTabs extends TabActivity {
	private Legislator legislator;

	private Database database;
	private Cursor cursor;
	
	ImageView star;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legislator);
        Bundle extras = getIntent().getExtras();
		legislator = (Legislator) extras.getSerializable("legislator");
		
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
		star.setOnClickListener(starClickListener);
		toggleFavoriteStar(cursor.getCount() == 1);

		TextView nameTitle = (TextView) findViewById(R.id.title_text);
		String titledName = legislator.titledName();
		nameTitle.setText(titledName);
		if (titledName.length() >= 23)
			nameTitle.setTextSize(19);
	}
	
	private View.OnClickListener starClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			toggleDatabaseFavorite();
		}
	};

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
			int result = database.removeLegislator(id);
			if (result != 0) {
				Utils.alert(this, R.string.legislator_favorites_removed);
				toggleFavoriteStar(false);
			}
		} else {
			long result = database.addLegislator(legislator);
			if (result != -1) {
				Utils.alert(this, R.string.legislator_favorites_added);
				toggleFavoriteStar(true);
			}
		}
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
