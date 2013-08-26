package com.sunlightlabs.android.congress;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;

public class NotificationSettings extends PreferenceActivity {
	private static final int EXPLANATION = 1;
	
	public static final String KEY_NOTIFY_ENABLED = "notify_enabled";
	public static final boolean DEFAULT_NOTIFY_ENABLED = false;

	public static final String KEY_NOTIFY_INTERVAL = "notify_interval";
	public static final String DEFAULT_NOTIFY_INTERVAL = "15";
	
	public static final String KEY_NOTIFY_RINGTONE = "notify_ringtone";
	public static final String DEFAULT_NOTIFY_RINGTONE = null;
	
	public static final String KEY_NOTIFY_VIBRATION = "notify_vibration";
	public static final boolean DEFAULT_NOTIFY_VIBRATION = true;
	
	// turned to false the first time the user ever visits this activity, and a dialog is shown explaining notifications 
	public static final String KEY_FIRST_TIME_SETTINGS = "first_time_settings";
	public static final boolean DEFAULT_FIRST_TIME_SETTINGS = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_bare);
		
		addPreferencesFromResource(R.xml.notification_settings);
		PreferenceManager.setDefaultValues(this, R.xml.notification_settings, false);
		
		if (firstTime()) {
			tripFirstTimeFlag();
			showDialog(EXPLANATION);
		}
		
		setupControls();
	}
	
	public void setupControls() {
		updateIntervalSummary(PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_NOTIFY_INTERVAL, DEFAULT_NOTIFY_INTERVAL));
		updateRingtoneSummary(PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_NOTIFY_RINGTONE, null));
		
		findPreference(KEY_NOTIFY_ENABLED).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = ((Boolean) newValue).booleanValue();
				if (value) {
					Utils.startNotificationsBroadcast(NotificationSettings.this);
					Log.d(Utils.TAG, "Prefs changed: START notification service");
				} else {
					Utils.stopNotificationsBroadcast(NotificationSettings.this);
					Log.d(Utils.TAG, "Prefs changed: STOP notification service");
				}
				
				return true;
			}
		});
		
		findPreference(KEY_NOTIFY_INTERVAL).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				updateIntervalSummary((String) newValue);
				Utils.stopNotificationsBroadcast(NotificationSettings.this);
				Utils.startNotificationsBroadcast(NotificationSettings.this);
				Log.d(Utils.TAG, "Prefs changed: RESTART notification service");
				return true;
			}
		});
		
		findPreference(KEY_NOTIFY_RINGTONE).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				updateRingtoneSummary((String) newValue);
				return true;
			}
		});
	}

	private void updateIntervalSummary(String newCode) {
		findPreference(KEY_NOTIFY_INTERVAL).setSummary("Check every " + codeToName(newCode));
	}
	
	private void updateRingtoneSummary(String uri) {
		String summary;
		
		if (uri != null && !uri.equals("")) {
			Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(uri));
			if (ringtone != null)
				summary = ringtone.getTitle(this);
			else
				summary = "Unknown";
		} else
			summary = "Silent";
		
		findPreference(KEY_NOTIFY_RINGTONE).setSummary(summary);
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
	
	private boolean firstTime() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_FIRST_TIME_SETTINGS, DEFAULT_FIRST_TIME_SETTINGS);
	}
	
	private void tripFirstTimeFlag() {
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(KEY_FIRST_TIME_SETTINGS, !DEFAULT_FIRST_TIME_SETTINGS).commit();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		
		if (id == EXPLANATION) {
			ViewGroup title = (ViewGroup) inflater.inflate(R.layout.alert_dialog_title, null);
			((TextView) title.findViewById(R.id.title)).setText(R.string.explanation_title);
			
			ViewGroup explanation = (ViewGroup) inflater.inflate(R.layout.explanation, null); 
			String explanation3a = "<b>&#183;</b> " + getString(R.string.explanation_3a); 
			((TextView) explanation.findViewById(R.id.explanation_3a)).setText(Html.fromHtml(explanation3a));
			String explanation3b = "<b>&#183;</b> " + getString(R.string.explanation_3b); 
			((TextView) explanation.findViewById(R.id.explanation_3b)).setText(Html.fromHtml(explanation3b));
			
			builder.setIcon(R.drawable.icon)
				.setCustomTitle(title)
				.setView(explanation)
				.setPositiveButton(R.string.explanation_button, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {}
				});
		}
		
		return builder.create();
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