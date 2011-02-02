package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.utils.Analytics;

public class Settings extends PreferenceActivity {
	
	public static final String ANALYTICS_ENABLED_KEY = "analytics_enabled";
	public static final boolean ANALYTICS_ENABLED_DEFAULT = true;
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		
		SettingsHolder holder = (SettingsHolder) getLastNonConfigurationInstance();
		if (holder != null)
			this.tracked = holder.tracked;
		
		tracker = Analytics.start(this);
		if (!tracked) {
			Analytics.page(this, tracker, "/settings");
			tracked = true;
		}
	}
	
	class SettingsHolder {
		boolean tracked;
		
		SettingsHolder(boolean tracked) {
			this.tracked = tracked;
		}
	}
}