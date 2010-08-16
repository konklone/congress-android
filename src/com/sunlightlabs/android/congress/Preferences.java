package com.sunlightlabs.android.congress;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.sunlightlabs.android.congress.notifications.Notifications;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_NOTIFY_ENABLED = "notify_enabled";
	public static final boolean DEFAULT_NOTIFY_ENABLED = false;

	public static final String KEY_NOTIFY_INTERVAL = "notify_interval";
	public static final String DEFAULT_NOTIFY_INTERVAL = "15";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		updateIntervalSummary();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(KEY_NOTIFY_ENABLED)) {

			if (sharedPreferences.getBoolean(key, DEFAULT_NOTIFY_ENABLED))
				Notifications.startNotificationsBroadcast(this);
			else
				Notifications.stopNotificationsBroadcast(this);

		} else if (key.equals(KEY_NOTIFY_INTERVAL)) {
			updateIntervalSummary();
			
			Notifications.stopNotificationsBroadcast(this);
			Notifications.startNotificationsBroadcast(this);
		}
	}
	
	private void updateIntervalSummary() {
		String newValue = getPreferenceScreen().getSharedPreferences().getString(KEY_NOTIFY_INTERVAL, DEFAULT_NOTIFY_INTERVAL);
		findPreference(KEY_NOTIFY_INTERVAL).setSummary(codeToName(newValue));
	}
	
	private String codeToName(String code) {
		String[] codes = getResources().getStringArray(R.array.notify_interval_codes);
		String[] names = getResources().getStringArray(R.array.notify_interval_names);

		for (int i=0; i<codes.length; i++) {
			if (codes[i].equals(code))
				return names[i];
		}
		return null;
	}
}