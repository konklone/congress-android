package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.os.Bundle;

import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class CommitteeListPager extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Analytics.init(this);
		setContentView(R.layout.pager_titled);
		
		ActionBarUtils.setTitle(this, "Committees");
		
		setupPager();
	}
	
	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("house", R.string.tab_house, CommitteeListFragment.forChamber("house"));
		adapter.add("senate", R.string.tab_senate, CommitteeListFragment.forChamber("senate"));
		adapter.add("joint", R.string.tab_joint, CommitteeListFragment.forChamber("joint"));
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