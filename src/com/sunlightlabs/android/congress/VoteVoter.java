package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;

import com.sunlightlabs.android.congress.fragments.RollListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.congress.models.Legislator;

public class VoteVoter extends FragmentActivity {
	
	Legislator legislator;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Analytics.track(this, "/votes/voter");
		
		legislator = (Legislator) getIntent().getSerializableExtra("legislator");
		
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		findViewById(R.id.pager_titles).setVisibility(View.GONE);
		
		adapter.add("votes_voter", "Not seen", RollListFragment.forLegislator(legislator));
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, "Latest Votes By\n" + legislator.titledName());
		ActionBarUtils.setTitleSize(this, 18);
	}
	
}