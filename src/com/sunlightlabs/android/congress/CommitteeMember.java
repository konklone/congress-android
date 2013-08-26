package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class CommitteeMember extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.frame_titled);
		
		Legislator legislator = (Legislator) getIntent().getExtras().getSerializable("legislator");
		
		ActionBarUtils.setTitle(this, "Committees for " + legislator.titledName(), Utils.legislatorIntent(this, legislator));
		ActionBarUtils.setTitleSize(this, 16);
		
		FragmentManager manager = getSupportFragmentManager();
		if (manager.findFragmentById(R.id.frame) == null)
			manager.beginTransaction().add(R.id.frame, CommitteeListFragment.forLegislator(legislator)).commit();
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