package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

import com.sunlightlabs.android.congress.utils.Utils;

public class CommitteeTabs extends TabActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.tabs_plain);

		setupControls();
		setupTabs();
	}

	private void setupControls() {
		Utils.setTitle(this, R.string.committees_title, R.drawable.committees);
	}

	private void setupTabs() {
		TabHost tabHost = getTabHost();

		Utils.addTab(this, tabHost, "committee_house", committeeIntent("house"), R.string.tab_house);
		Utils.addTab(this, tabHost, "committee_senate", committeeIntent("senate"), R.string.tab_senate);
		Utils.addTab(this, tabHost, "committee_joint", committeeIntent("joint"), R.string.tab_joint);

		tabHost.setCurrentTab(0);
	}

	private Intent committeeIntent(String chamber) {
		return new Intent(this, CommitteeList.class)
			.putExtra("chamber", chamber)
			.putExtra("type", CommitteeList.CHAMBER);
	}
}