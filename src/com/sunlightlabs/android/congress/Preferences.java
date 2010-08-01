package com.sunlightlabs.android.congress;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.sunlightlabs.android.congress.notifications.Notifications;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_NOTIFY_ENABLED = "congress.notify_enabled";
	public static final boolean DEFAULT_NOTIFY_ENABLED = false;

	public static final String KEY_NOTIFY_INTERVAL = "congress.notify_interval";
	public static final String DEFAULT_NOTIFY_INTERVAL = "15";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(KEY_NOTIFY_ENABLED)) {

			if (sharedPreferences.getBoolean(key, DEFAULT_NOTIFY_ENABLED))
				Notifications.startNotificationsBroadcast(this);
			else
				Notifications.stopNotificationsBroadcast(this);

		} else if (key.equals(KEY_NOTIFY_INTERVAL)) {
			Notifications.stopNotificationsBroadcast(this);
			Notifications.startNotificationsBroadcast(this);
		}
	}
}
