package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;

public class Settings extends PreferenceActivity {
	
	public static final String ANALYTICS_ENABLED_KEY = "analytics_enabled";
	public static final boolean ANALYTICS_ENABLED_DEFAULT = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled);
		
		ActionBarUtils.setTitle(this, R.string.menu_settings);
		
		addPreferencesFromResource(R.xml.settings);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		
		findPreference(ANALYTICS_ENABLED_KEY).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = ((Boolean) newValue).booleanValue();
				
				// if user is disabling analytics, fire off one last event so that we can get an idea of how many are disabling it
				if (!value)
					Analytics.analyticsDisable(Settings.this);
				
				return true;
			}
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Analytics.start(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Analytics.stop(this);
	}
}