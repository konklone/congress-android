package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;

public class CommitteePager extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.pager_titled);
		
		Analytics.track(this, "/committees");
		
		Utils.setTitle(this, "Committees");
		
		setupPager();
	}
	
	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("house", R.string.tab_house, CommitteeListFragment.forChamber("house"));
		adapter.add("senate", R.string.tab_senate, CommitteeListFragment.forChamber("senate"));
		adapter.add("joint", R.string.tab_joint, CommitteeListFragment.forChamber("joint"));
	}
}