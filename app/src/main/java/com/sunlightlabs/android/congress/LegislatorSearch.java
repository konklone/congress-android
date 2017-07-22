package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.LegislatorListFragment;
import com.sunlightlabs.android.congress.providers.SuggestionsProvider;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;

public class LegislatorSearch extends Activity {
	
	String query;
	String state;
	
	TitlePageAdapter adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Analytics.init(this);
		setContentView(R.layout.pager_titled);
		
		
		Intent intent = getIntent();
		query = intent.getStringExtra(SearchManager.QUERY);
		state = intent.getStringExtra("state");
		
		if (query != null) // may come in from the state list
			query = query.trim();
	    
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		adapter = new TitlePageAdapter(this);
		findViewById(R.id.pager_titles).setVisibility(View.GONE);

		// state search
		if (state != null) {
			ActionBarUtils.setTitle(this, "Legislators from " + Utils.stateCodeToName(this, state));
			ActionBarUtils.setTitleSize(this, 16);
			adapter.add("legislators_state", "Not seen", LegislatorListFragment.forState(state));
		}
		
		// last name search
		else {
			ActionBarUtils.setTitle(this, "Legislators Named \"" + query + "\"");
			ActionBarUtils.setTitleSize(this, 16);
			
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this, SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
	        suggestions.saveRecentQuery(query, null);
			
			adapter.add("legislators_lastname", "Not seen", LegislatorListFragment.forLastName(query));
		}
		
		ActionBarUtils.setTitleButton(this, new Intent(this, MenuLegislators.class));
	}
	
	public void setupControls() {
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.search, new View.OnClickListener() {
			public void onClick(View v) { 
				onSearchRequested();
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		Analytics.start(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Analytics.stop(this);
	}
	
}