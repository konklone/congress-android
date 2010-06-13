package com.sunlightlabs.android.congress;

import com.sunlightlabs.android.congress.R;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {

	@Override 
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState); 
		addPreferencesFromResource(R.xml.preferences);
	}
	
	public static String getString(Context context, String key) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, null);
	}
	
	public static boolean setString(Context context, String key, String value) {
		return PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).commit();
	}
	
	public static boolean getBoolean(Context context, String key, boolean defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
	}
	
	public static boolean setBoolean(Context context, String key, boolean value) {
		return PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).commit();
	}
	
}