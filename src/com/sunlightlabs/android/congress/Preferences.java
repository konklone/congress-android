package com.sunlightlabs.android.congress;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_NOTIFY_ENABLED = "notify_enabled";
	public static final boolean DEFAULT_NOTIFY_ENABLED = false;

	public static final String KEY_NOTIFY_INTERVAL = "notify_interval";
	public static final String DEFAULT_NOTIFY_INTERVAL = "1";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
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

			if (sharedPreferences.getBoolean(key, DEFAULT_NOTIFY_ENABLED)) {
				Utils.startNotificationsBroadcast(this);
				Log.d(Utils.TAG, "Prefs changed: START notification service");
			} else {
				Utils.stopNotificationsBroadcast(this);
				Log.d(Utils.TAG, "Prefs changed: STOP notification service");
			}

		} else if (key.equals(KEY_NOTIFY_INTERVAL)) {
			Utils.stopNotificationsBroadcast(this);
			Utils.startNotificationsBroadcast(this);
			Log.d(Utils.TAG, "Prefs changed: RESTART notification service");
		}
	}


}
