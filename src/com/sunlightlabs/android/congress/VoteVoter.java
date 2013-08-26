package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.RollListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class VoteVoter extends FragmentActivity {
	
	Legislator legislator;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
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
		ActionBarUtils.setTitle(this, "Latest Votes By\n" + legislator.titledName(), Utils.legislatorIntent(this, legislator));
		ActionBarUtils.setTitleSize(this, 16);
	}
	
	@Override
	public boolean onSearchRequested() {
		Bundle args = new Bundle();
		args.putSerializable("legislator", legislator);
		startSearch(null, false, args, false);
		return true;
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