package com.sunlightlabs.android.congress;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;

public class NotificationSettings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_NOTIFY_ENABLED = "notify_enabled";
	public static final boolean DEFAULT_NOTIFY_ENABLED = false;

	public static final String KEY_NOTIFY_INTERVAL = "notify_interval";
	public static final String DEFAULT_NOTIFY_INTERVAL = "15";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.list_titled);
		Utils.setTitle(this, R.string.menu_notification_settings);
		
		addPreferencesFromResource(R.xml.preferences);
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

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

			if (sharedPreferences.getBoolean(key, DEFAULT_NOTIFY_ENABLED)) {
				Utils.startNotificationsBroadcast(this);
				Log.d(Utils.TAG, "Prefs changed: START notification service");
			} else {
				Utils.stopNotificationsBroadcast(this);
				Log.d(Utils.TAG, "Prefs changed: STOP notification service");
			}

		} else if (key.equals(KEY_NOTIFY_INTERVAL)) {
			updateIntervalSummary();
			Utils.stopNotificationsBroadcast(this);
			Utils.startNotificationsBroadcast(this);
			Log.d(Utils.TAG, "Prefs changed: RESTART notification service");
		}
	}
	
	private void updateIntervalSummary() {
		String newValue = PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_NOTIFY_INTERVAL, DEFAULT_NOTIFY_INTERVAL);
		findPreference(KEY_NOTIFY_INTERVAL).setSummary("Check every " + codeToName(newValue));
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
