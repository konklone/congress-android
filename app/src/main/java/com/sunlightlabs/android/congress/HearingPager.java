package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.HearingListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class HearingPager extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Analytics.init(this);
		setContentView(R.layout.pager_titled);
			
		setupControls();
		setupPager();
	}

	private void setupPager() {
        findViewById(R.id.pager_titles).setVisibility(View.GONE);

		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("upcoming", R.string.tab_upcoming_hearings, HearingListFragment.upcoming());
		
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