package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class CommitteeMember extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Analytics.init(this);
		setContentView(R.layout.frame_titled);
		
		Legislator legislator = (Legislator) getIntent().getExtras().getSerializable("legislator");
		
		ActionBarUtils.setTitle(this, "Committees for " + legislator.titledName(), Utils.legislatorIntent(this, legislator));
		ActionBarUtils.setTitleSize(this, 16);
		
		FragmentManager manager = getFragmentManager();
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