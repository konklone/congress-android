package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;

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
		TextView title = (TextView) findViewById(R.id.title_text);
		title.setText(getString(R.string.committees_title));
	}

	private void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();

		Utils.addTab(this, tabHost, "committee_house", committeeIntent("house"), "House", res.getDrawable(R.drawable.committee));
		Utils.addTab(this, tabHost, "committee_senate", committeeIntent("senate"), "Senate", res.getDrawable(R.drawable.committee));
		Utils.addTab(this, tabHost, "committee_joint", committeeIntent("joint"), "Joint", res.getDrawable(R.drawable.committee));

		tabHost.setCurrentTab(0);
	}

	private Intent committeeIntent(String chamber) {
		return new Intent(this, CommitteeList.class).putExtra("chamber", chamber);
	}
}