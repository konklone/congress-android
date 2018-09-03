package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.os.Bundle;

import com.sunlightlabs.android.congress.fragments.FloorUpdateFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class FloorUpdatePager extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Analytics.init(this);
		setContentView(R.layout.pager_titled);

		ActionBarUtils.setTitle(this, R.string.floor_updates_title);
		setupPager();
	}

	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("house", R.string.tab_house, FloorUpdateFragment.forChamber("house"));
		adapter.add("senate", R.string.tab_senate, FloorUpdateFragment.forChamber("senate"));

		String chamber = getIntent().getStringExtra("chamber");
		if (chamber != null && chamber.equals("senate"))
			adapter.selectPage(1);
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