package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class CommitteeList extends FragmentActivity {
	public static final int CHAMBER = 1;
	public static final int LEGISLATOR = 2;
	
	private int type;
	private String chamber;
	private Legislator legislator;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		Bundle extras = getIntent().getExtras();
		
		type = extras.getInt("type");
		chamber = extras.getString("chamber");
		legislator = (Legislator) extras.getSerializable("legislator");
		
		setupControls();
		
		if (type == CHAMBER)
			Analytics.track(this, "/committees/" + chamber);
		else if (type == LEGISLATOR)
			Analytics.track(this, "/legislator/" + legislator.id + "/committees");
			
		if (getSupportFragmentManager().findFragmentById(R.id.frame) == null) {
			CommitteeListFragment list;
			
			if (type == LEGISLATOR)
				list = CommitteeListFragment.forLegislator(legislator.id);
			else // if (type == CHAMBER)
				list = CommitteeListFragment.forChamber(chamber);
			
            getSupportFragmentManager().beginTransaction().add(R.id.frame, list).commit();
        }
	}
	
	private void setupControls() {
		if (type == LEGISLATOR) {
			setContentView(R.layout.frame_titled);
			Utils.setTitle(this, "Committees for " + legislator.titledName(), R.drawable.committees);
			Utils.setTitleSize(this, 18);
		} else
			setContentView(R.layout.frame);
	}
}