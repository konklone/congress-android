package com.sunlightlabs.android.congress;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.fragments.LegislatorListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.congress.models.Committee;

public class CommitteePager extends FragmentActivity {
	
	Committee committee;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		committee = (Committee) getIntent().getSerializableExtra("committee");
		
		setupControls();
		setupPager();
	}
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("committee_members", R.string.tab_committees_members, LegislatorListFragment.forCommittee(committee));
		
		if (committee.subcommittee)
			findViewById(R.id.pager_titles).setVisibility(View.GONE);
		else
			adapter.add("committee_subcommittees", R.string.tab_committees_sub, CommitteeListFragment.forCommittee(committee));
	}
	
	public void setupControls() {
		String name = committee.name;
		
		if (committee.subcommittee)
			name = "Subcommittee on " + name;
		
		ActionBarUtils.setTitle(this, name, new Intent(this, CommitteeListPager.class));
		ActionBarUtils.setTitleSize(this, 16);
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