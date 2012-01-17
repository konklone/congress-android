package com.sunlightlabs.android.congress;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.LegislatorListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class LegislatorCommittee extends FragmentActivity {
	
	String committeeId, committeeName;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Analytics.track(this, "/committee/legislators");
		
		Intent intent = getIntent();
		committeeId = intent.getStringExtra("committeeId");
		committeeName = intent.getStringExtra("committeeName");
		
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		findViewById(R.id.pager_titles).setVisibility(View.GONE);
		
		adapter.add("legislator_committee", "Not seen", LegislatorListFragment.forCommittee(committeeId, committeeName));
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, committeeName, new Intent(this, CommitteePager.class));
		ActionBarUtils.setTitleSize(this, 16);
	}
}