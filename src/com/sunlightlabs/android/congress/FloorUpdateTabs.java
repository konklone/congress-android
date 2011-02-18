package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

import com.sunlightlabs.android.congress.utils.Utils;

public class FloorUpdateTabs extends TabActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tabs_plain);

		setupControls();
		setupTabs();
	}
	
	private void setupControls() {
		Utils.setTitle(this, R.string.menu_main_floor_updates, R.drawable.people);
	}

	private void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();

		Utils.addTab(this, tabHost, "house", floorUpdatesIntent("house"), "House", res.getDrawable(R.drawable.people));
		Utils.addTab(this, tabHost, "senate", floorUpdatesIntent("senate"), "Senate", res.getDrawable(R.drawable.people));

		tabHost.setCurrentTab(0);
	}

	private Intent floorUpdatesIntent(String chamber) {
		return new Intent(this, FloorUpdateList.class).putExtra("chamber", chamber);
	}
	
}