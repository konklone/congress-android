package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;

public class Settings extends PreferenceActivity {
	
	public static final String ANALYTICS_ENABLED_KEY = "analytics_enabled";
	public static final boolean ANALYTICS_ENABLED_DEFAULT = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Analytics.init(this);
		setContentView(R.layout.list_titled);
		
		ActionBarUtils.setTitle(this, R.string.menu_settings);
		
		addPreferencesFromResource(R.xml.settings);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		findPreference(ANALYTICS_ENABLED_KEY).setOnPreferenceChangeListener((preference, newValue) -> {
			boolean yesToAnalytics = (Boolean) newValue;

			// if user is disabling analytics, fire off one last event so that we can get an idea of how many are disabling it
			if (!yesToAnalytics)
				Analytics.analyticsDisable(Settings.this);
			Analytics.optout(Settings.this, !yesToAnalytics);

			return true;
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