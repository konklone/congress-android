package com.sunlightlabs.android.congress;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.LegislatorProfileFragment;
import com.sunlightlabs.android.congress.fragments.NewsListFragment;
import com.sunlightlabs.android.congress.fragments.TweetsFragment;
import com.sunlightlabs.android.congress.fragments.YouTubeFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.ActionBarUtils.HasActionMenu;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorPager extends FragmentActivity implements HasActionMenu {
	private Legislator legislator;
	private String tab;
	private Database database;
	private Cursor cursor;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Bundle extras = getIntent().getExtras();
		legislator = (Legislator) extras.getSerializable("legislator");
		tab = extras.getString("tab");
		
		Analytics.track(this, "/legislator?bioguide_id=" + legislator.id);
		
		setupDatabase();
		setupControls();
		setupPager();	
	}
	
	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("info", R.string.tab_profile, LegislatorProfileFragment.create(legislator));
		adapter.add("news", R.string.tab_news, NewsListFragment.forLegislator(legislator));
		
		if (legislator.twitter_id != null && !legislator.twitter_id.equals(""))
			adapter.add("tweets", R.string.tab_tweets, TweetsFragment.create(legislator));
		
		if (legislator.youtube_url != null && !legislator.youtube_url.equals(""))
			adapter.add("videos", R.string.tab_videos, YouTubeFragment.create(legislator));
		
		if (tab != null) adapter.selectPage(tab);
	}
	
	private void setupDatabase() {
		database = new Database(this);
		database.open();
		cursor = database.getLegislator(legislator.getId());
		startManagingCursor(cursor);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}
	
	public void setupControls() {
		String titledName = legislator.titledName();
		ActionBarUtils.setTitle(this, titledName);
		if (titledName.length() >= 23)
			ActionBarUtils.setTitleSize(this, 16);
		
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.star_off, new View.OnClickListener() {
			public void onClick(View v) { 
				toggleDatabaseFavorite(); 
			}
		});
		
		toggleFavoriteStar(cursor.getCount() == 1);
		
		ActionBarUtils.setActionMenu(this, R.menu.legislator);
	}

	private void toggleFavoriteStar(boolean enabled) {
		if (enabled)
			ActionBarUtils.setActionIcon(this, R.id.action_1, R.drawable.star_on);
		else
			ActionBarUtils.setActionIcon(this, R.id.action_1, R.drawable.star_off);
	}

	private void toggleDatabaseFavorite() {
		String id = legislator.getId();
		cursor.requery();
		if (cursor.getCount() == 1) {
			if (database.removeLegislator(id) != 0) {
				toggleFavoriteStar(false);
				Analytics.removeFavoriteLegislator(this, id);
			} else
				Utils.alert(this, "Problem unstarring legislator.");
		} else {
			if (database.addLegislator(legislator) != -1) {
				toggleFavoriteStar(true);
				Analytics.addFavoriteLegislator(this, id);
			} else
				Utils.alert(this, "Problem starring legislator.");
		}
	}
	
	@Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
		super.onCreateOptionsMenu(menu); 
	    getMenuInflater().inflate(R.menu.legislator, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	menuSelected(item);
    	return true;
    }
	
	@Override
	public void menuSelected(MenuItem item) {
		switch(item.getItemId()) {
    	case R.id.govtrack:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.govTrackUrl(legislator.govtrack_id))));
    		break;
    	case R.id.opencongress:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.openCongressUrl(legislator.govtrack_id))));
    		break;
    	case R.id.bioguide:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.bioguideUrl(legislator.bioguide_id))));
    		break;
    	}
	}
}