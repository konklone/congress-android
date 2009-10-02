package com.sunlightlabs.android.congress;

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
	
	public static String getString(Context context, String value) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(value, null);
	}
	
}