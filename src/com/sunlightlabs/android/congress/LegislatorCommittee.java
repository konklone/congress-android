package com.sunlightlabs.android.congress;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.LegislatorListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.congress.models.Committee;

public class LegislatorCommittee extends FragmentActivity {
	
	Committee committee;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Analytics.track(this, "/committee/legislators");
		
		committee = (Committee) getIntent().getSerializableExtra("committee");
		
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		findViewById(R.id.pager_titles).setVisibility(View.GONE);
		
		adapter.add("legislator_committee", "Not seen", LegislatorListFragment.forCommittee(committee));
	}
	
	public void setupControls() {
		String name = committee.name;
		
		if (committee.subcommittee)
			name = "Subcommittee on " + name;
		
		ActionBarUtils.setTitle(this, name, new Intent(this, CommitteeListPager.class));
		ActionBarUtils.setTitleSize(this, 16);
	}
}