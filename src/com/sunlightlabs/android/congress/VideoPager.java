package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.sunlightlabs.android.congress.fragments.VideoFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class VideoPager extends FragmentActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
			
		Analytics.track(this, "/video");
		
		ActionBarUtils.setTitle(this, "Watch");
		setupPager();
	}

	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("house", R.string.tab_house, VideoFragment.forChamber("house"));
	}
}
