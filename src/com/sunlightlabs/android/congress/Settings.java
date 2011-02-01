package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity {
	
	public static final String ANALYTICS_ENABLED_KEY = "analytics_enabled";
	public static final boolean ANALYTICS_ENABLED_DEFAULT = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
	}
}