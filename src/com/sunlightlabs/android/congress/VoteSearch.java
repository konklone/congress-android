package com.sunlightlabs.android.congress;

import android.app.SearchManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.sunlightlabs.android.congress.fragments.RollListFragment;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;

public class VoteSearch extends FragmentActivity {
	
	String query;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		query = getIntent().getStringExtra(SearchManager.QUERY).trim();
		
		Analytics.track(this, "/votes/search");
	    
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("votes_recent", R.string.search_votes_recent, RollListFragment.forSearch(query, RollListFragment.ROLLS_SEARCH_NEWEST));
		adapter.add("votes_relevant", R.string.search_votes_relevant, RollListFragment.forSearch(query, RollListFragment.ROLLS_SEARCH_RELEVANT));
	}

	public void setupControls() {
		Utils.setTitle(this, "Votes matching \"" + query + "\"");
		Utils.setTitleSize(this, 16);
	}
	
}