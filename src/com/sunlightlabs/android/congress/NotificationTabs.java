package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

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
		Utils.setTitle(this, R.string.menu_notification_settings, R.drawable.notifications);
	}

	private void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();

		Utils.addTab(this, tabHost, "settings", settingsIntent(), "Settings", res.getDrawable(android.R.drawable.ic_menu_preferences));
		Utils.addTab(this, tabHost, "subscriptions", subscriptionsIntent(), "Subscriptions", res.getDrawable(R.drawable.notifications));

		tabHost.setCurrentTab(0);
	}

	private Intent settingsIntent() {
		return new Intent(this, NotificationSettings.class);
	}
	
	private Intent subscriptionsIntent() {
		return new Intent(this, NotificationSubscriptions.class);
	}
}