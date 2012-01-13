package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Utils;

public class NotificationTabs extends TabActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tabs_plain);

		setupControls();
		setupTabs();
	}

	private void setupControls() {
		ActionBarUtils.setTitle(this, R.string.menu_notification_settings);
	}

	private void setupTabs() {
		TabHost tabHost = getTabHost();

		Utils.addTab(this, tabHost, "settings", settingsIntent(), R.string.tab_settings);
		Utils.addTab(this, tabHost, "subscriptions", subscriptionsIntent(), R.string.tab_subscriptions);

		tabHost.setCurrentTab(0);
	}

	private Intent settingsIntent() {
		return new Intent(this, NotificationSettings.class);
	}
	
	private Intent subscriptionsIntent() {
		return new Intent(this, NotificationSubscriptions.class);
	}
}