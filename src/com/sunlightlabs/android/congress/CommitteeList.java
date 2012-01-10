package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Window;

import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class CommitteeList extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.frame_titled);
		
		Legislator legislator = (Legislator) getIntent().getExtras().getSerializable("legislator");
		
		Analytics.track(this, "/legislator/committees?bioguide_id=" + legislator.id);
		
		Utils.setTitle(this, "Committees for " + legislator.titledName());
		Utils.setTitleSize(this, 16);
		
		FragmentManager manager = getSupportFragmentManager();
		if (manager.findFragmentById(R.id.frame) == null)
			manager.beginTransaction().add(R.id.frame, CommitteeListFragment.forLegislator(legislator)).commit();
		
	}
}