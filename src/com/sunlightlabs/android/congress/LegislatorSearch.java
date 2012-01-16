package com.sunlightlabs.android.congress;

import java.util.regex.Pattern;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.LegislatorListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;

public class LegislatorSearch extends FragmentActivity {
	
	String query;
	String state;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Intent intent = getIntent();
		query = intent.getStringExtra(SearchManager.QUERY);
		state = intent.getStringExtra("state");
		
		if (query != null) // may come in from the state list
			query = query.trim();
	    
		setupPager();
		setupControls();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		findViewById(R.id.pager_titles).setVisibility(View.GONE);
		
		// state search
		if (state != null) {
			Analytics.track(this, "/legislators/state");
			ActionBarUtils.setTitle(this, "Legislators from " + Utils.stateCodeToName(this, state));
			adapter.add("legislators_state", "Not seen", LegislatorListFragment.forState(state));
		}
		
		// zip code search
		else if (Pattern.compile("^\\d+$").matcher(query).matches()) {
			Analytics.track(this, "/legislators/zip");
			ActionBarUtils.setTitle(this, "Legislators For " + query);
			adapter.add("legislators_zip", "Not seen", LegislatorListFragment.forZip(query));
		}
		
		// last name search
		else {
			Analytics.track(this, "/legislators/lastname");
			ActionBarUtils.setTitle(this, "Legislators Named \"" + query + "\"");
			adapter.add("legislators_lastname", "Not seen", LegislatorListFragment.forLastName(query));
		}
	}
	
	public void setupControls() {
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.search, new View.OnClickListener() {
			public void onClick(View v) { 
				onSearchRequested();
			}
		});
	}
	
		
}