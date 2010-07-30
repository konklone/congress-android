package com.sunlightlabs.android.congress;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.sunlightlabs.android.congress.notifications.Notifications;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_NOTIFICATIONS_ENABLED = "congress.notifications_enabled";
	public static final boolean DEFAULT_NOTIFICATIONS_ENABLED = false;

	public static final String KEY_NOTIFICATIONS_INTERVAL = "congress.notifications_interval";
	public static final String DEFAULT_NOTIFICATIONS_INTERVAL = "15";

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
		if (key.equals(KEY_NOTIFICATIONS_ENABLED)) {
			if (sharedPreferences.getBoolean(key, DEFAULT_NOTIFICATIONS_ENABLED))
				Notifications.scheduleNotifications(this);
			else
				Notifications.stopNotifications(this);
		} else if (key.equals(KEY_NOTIFICATIONS_INTERVAL)) {
			Notifications.stopNotifications(this);
			Notifications.scheduleNotifications(this);
		}
	}
}
