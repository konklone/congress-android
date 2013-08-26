package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.sunlightlabs.android.congress.fragments.HearingListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class HearingPager extends FragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
			
		setupControls();
		setupPager();
	}

	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("house", R.string.tab_house, HearingListFragment.forChamber("house"));
		adapter.add("senate", R.string.tab_senate, HearingListFragment.forChamber("senate"));
		
		String chamber = getIntent().getStringExtra("chamber");
		if (chamber != null && chamber.equals("senate"))
			adapter.selectPage(1);
	}
	
	private void setupControls() {
		ActionBarUtils.setTitle(this, R.string.hearings_title);
		ActionBarUtils.setTitleSize(this, 18);
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