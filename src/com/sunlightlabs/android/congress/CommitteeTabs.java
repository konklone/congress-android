package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

import com.sunlightlabs.android.congress.utils.Utils;

public class CommitteeTabs extends TabActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.committee);

		setupControls();
		setupTabs();
	}

	private void setupControls() {
		Utils.setTitle(this, R.string.committees_title, R.drawable.people);
	}

	private void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();

		Utils.addTab(this, tabHost, "committee_house", committeeIntent("house"), "House", res.getDrawable(R.drawable.people));
		Utils.addTab(this, tabHost, "committee_senate", committeeIntent("senate"), "Senate", res.getDrawable(R.drawable.people));
		Utils.addTab(this, tabHost, "committee_joint", committeeIntent("joint"), "Joint", res.getDrawable(R.drawable.people));

		tabHost.setCurrentTab(0);
	}

	private Intent committeeIntent(String chamber) {
		return new Intent(this, CommitteeList.class).putExtra("chamber", chamber);
	}
}