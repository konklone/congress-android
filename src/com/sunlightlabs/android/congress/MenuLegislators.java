package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.LegislatorListFragment;
import com.sunlightlabs.android.congress.fragments.MenuLegislatorsFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class MenuLegislators extends FragmentActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Analytics.track(this, "/legislators");
		
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("legislators_menu", R.string.menu_legislators_following, MenuLegislatorsFragment.newInstance());
		adapter.add("legislators_house", R.string.menu_legislators_house, LegislatorListFragment.forChamber("house"));
		adapter.add("legislators_senate", R.string.menu_legislators_senate, LegislatorListFragment.forChamber("senate"));
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, R.string.menu_main_legislators);
		
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.search, new View.OnClickListener() {
			public void onClick(View v) { 
				onSearchRequested();
			}
		});
	}
}