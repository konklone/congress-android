package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class CommitteePager extends FragmentActivity {
	public static final int CHAMBERS = 1;
	public static final int LEGISLATOR = 2;
	
	private int type;
	private Legislator legislator;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		Bundle extras = getIntent().getExtras();
		
		type = extras.getInt("type");
		legislator = (Legislator) extras.getSerializable("legislator");
		
		if (type == LEGISLATOR) {
			setContentView(R.layout.frame_titled);
			
			Analytics.track(this, "/legislator/committees?bioguide_id=" + legislator.id);
			
			Utils.setTitle(this, "Committees for " + legislator.titledName());
			Utils.setTitleSize(this, 16);
			getSupportFragmentManager().beginTransaction().add(R.id.frame, CommitteeListFragment.forLegislator(legislator)).commit();
		} else {
			setContentView(R.layout.pager_titled);
			Utils.setTitle(this, "Committees");
			
			Analytics.track(this, "/committees");
			
			setupPager();
		}	
	}
	
	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("house", R.string.tab_house, CommitteeListFragment.forChamber("house"));
		adapter.add("senate", R.string.tab_senate, CommitteeListFragment.forChamber("senate"));
		adapter.add("joint", R.string.tab_joint, CommitteeListFragment.forChamber("joint"));
	}
}