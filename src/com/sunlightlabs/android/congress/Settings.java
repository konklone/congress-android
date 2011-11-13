package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;

public class Settings extends PreferenceActivity {
	
	public static final String ANALYTICS_ENABLED_KEY = "analytics_enabled";
	public static final boolean ANALYTICS_ENABLED_DEFAULT = true;
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled);
		Utils.setTitle(this, R.string.menu_settings);
		
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
		
		findPreference(ANALYTICS_ENABLED_KEY).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = ((Boolean) newValue).booleanValue();
				
				// if user is disabling analytics, fire off one last event so that we can get an idea of how many are disabling it
				if (!value)
					Analytics.analyticsDisable(Settings.this, tracker);
				
				return true;
			}
		});
	}
	
	class SettingsHolder {
		boolean tracked;
		
		SettingsHolder(boolean tracked) {
			this.tracked = tracked;
		}
	}
}