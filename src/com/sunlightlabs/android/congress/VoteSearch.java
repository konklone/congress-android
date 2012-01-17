package com.sunlightlabs.android.congress;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.RollListFragment;
import com.sunlightlabs.android.congress.providers.SuggestionsProvider;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class VoteSearch extends FragmentActivity {
	
	String query;
	Legislator voter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Intent intent = getIntent();
		query = intent.getStringExtra(SearchManager.QUERY).trim();
		
		Bundle searchData = intent.getBundleExtra(SearchManager.APP_DATA);
		if (searchData != null)
			voter = (Legislator) searchData.getSerializable("legislator");
		
		Analytics.track(this, "/votes/search");
	    
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
				this, SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
        suggestions.saveRecentQuery(query, null);
		
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("votes_recent", R.string.search_votes_recent, RollListFragment.forSearch(query, voter, RollListFragment.ROLLS_SEARCH_NEWEST));
		adapter.add("votes_relevant", R.string.search_votes_relevant, RollListFragment.forSearch(query, voter, RollListFragment.ROLLS_SEARCH_RELEVANT));
	}

	public void setupControls() {
		if (voter != null) {
			ActionBarUtils.setTitle(this, "Votes by " + voter.titledName() + " matching \"" + query + "\"", Utils.legislatorIntent(this, voter));
			ActionBarUtils.setTitleSize(this, 14);
		} else {
			ActionBarUtils.setTitle(this, "Votes matching \"" + query + "\"", new Intent(this, MenuVotes.class));
			ActionBarUtils.setTitleSize(this, 16);
		}
		
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.search, new View.OnClickListener() {
			public void onClick(View v) { 
				onSearchRequested();
			}
		});
	}
	
	@Override
	public boolean onSearchRequested() {
		Bundle args = new Bundle();
		args.putSerializable("legislator", voter);
		startSearch(null, false, args, false);
		return true;
	}
	
}