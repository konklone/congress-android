package com.sunlightlabs.android.congress.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.sunlightlabs.android.congress.BillTabs;
import com.sunlightlabs.android.congress.LegislatorTabs;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.RollInfo;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.Drumbone;
import com.sunlightlabs.congress.services.Sunlight;

public class Utils {
	private static Method setView = null;
	
	public static void setupDrumbone(Context context) {
		Resources resources = context.getResources();
		Drumbone.userAgent = resources.getString(R.string.drumbone_user_agent);
		Drumbone.apiKey = resources.getString(R.string.sunlight_api_key);
		Drumbone.appVersion = resources.getString(R.string.app_version);
	}

	public static void setupSunlight(Context context) {
		Resources resources = context.getResources();
		Sunlight.apiKey = resources.getString(R.string.sunlight_api_key);
		Sunlight.appVersion = resources.getString(R.string.app_version);
	}

	public static void alert(Context context, String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}

	public static void alert(Context context, int msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}

	public static void alert(Context context, CongressException exception) {
		String message = exception == null ? "Unhandled error." : exception.getMessage();
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	// Suitable for a legislator desktop shortcut, load a legislator by ID only
	public static Intent legislatorIntent(String id) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorLoader");
		intent.putExtra("legislator_id", id); 
		return intent;
	}

	// Suitable for a direct link to a legislator, bypassing the
	// LegislatorLoader entirely
	public static Intent legislatorIntent(Context context, Legislator legislator) {
		return new Intent(context, LegislatorTabs.class).putExtra("legislator", legislator);
	}

	public static Intent legislatorIntent(Context context, Class<?> activityClass, Legislator legislator) {
		return new Intent(context, activityClass).putExtra("legislator", legislator);
	}

	// Suitable for going from a list to the bill display page.
	// Assumes the "basic" section of a bill has been loaded.
	// Not suitable for shortcut intents.

	// so tedious that I want a method to load up an Intent for any class
	public static Intent billIntent(Context context, Bill bill) {
		return billIntent(context, BillTabs.class, bill);
	}

	public static Intent billIntent(Context context, Class<?> cls, Bill bill) {
		return new Intent(context, cls).putExtra("bill", bill);
	}

	public static Intent billIntent(String billId, String code) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.BillLoader");
		intent.putExtra("extra", false)
			.putExtra("id", billId)
			.putExtra("code", code);
		return intent;
	}

	public static Intent billIntent(String billId) {
		return billIntent(billId, null);
	}

	public static Intent rollIntent(Context context, String rollId) {
		return new Intent(context, RollInfo.class)
		.putExtra("id", rollId);
	}

	public static Intent shortcutIntent(Context context, String billId, String code) {
		Parcelable resource = Intent.ShortcutIconResource.fromContext(context, R.drawable.bill);
		return new Intent()
		.putExtra(Intent.EXTRA_SHORTCUT_INTENT, 
				billIntent(billId, code).addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
				.putExtra(Intent.EXTRA_SHORTCUT_NAME, Bill.formatCodeShort(code))
				.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, resource);
	}

	public static Intent shortcutIntent(Context context, Legislator legislator, Bitmap icon) {
		return shortcutIntent(context, legislator.bioguide_id, legislator.last_name, icon);
	}

	public static Intent shortcutIntent(Context context, String legislatorId, String name, Bitmap icon) {
		Intent intent = new Intent()
		.putExtra(Intent.EXTRA_SHORTCUT_INTENT, 
				Utils.legislatorIntent(legislatorId).addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
				.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);

		if (icon != null)
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
		else {
			Parcelable resource = Intent.ShortcutIconResource.fromContext(context, R.drawable.no_photo_male);
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, resource);
		}

		return intent;
	}

	public static String stateCodeToName(Context context, String code) {
		String[] codes = context.getResources().getStringArray(R.array.state_codes);
		String[] names = context.getResources().getStringArray(R.array.state_names);

		for (int i=0; i<codes.length; i++) {
			if (codes[i].equals(code))
				return names[i];
		}
		return null;
	}

	public static String stateNameToCode(Context context, String name) {
		String[] codes = context.getResources().getStringArray(R.array.state_codes);
		String[] names = context.getResources().getStringArray(R.array.state_names);

		for (int i=0; i<names.length; i++) {
			if (names[i].equals(name))
				return codes[i];
		}
		return null;
	}

	public static int stateNameToPosition(Context context, String name) {
		String[] names = context.getResources().getStringArray(R.array.state_names);

		for (int i=0; i<names.length; i++) {
			if (names[i].equals(name))
				return i;
		}
		return 0;
	}

	public static String truncate(String text, int length) {
		if (text.length() > length)
			return text.substring(0, length - 3) + "...";
		else
			return text;
	}

	public static void showLoading(Activity activity) {
		activity.findViewById(R.id.empty_message).setVisibility(View.GONE);
		activity.findViewById(R.id.refresh).setVisibility(View.GONE);
		activity.findViewById(R.id.loading).setVisibility(View.VISIBLE);
	}

	public static void setLoading(Activity activity, int message) {
		((TextView) activity.findViewById(R.id.loading_message)).setText(message);
	}

	public static void showRefresh(Activity activity, int message) {
		activity.findViewById(R.id.loading).setVisibility(View.GONE);
		TextView messageView = (TextView) activity.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
		activity.findViewById(R.id.refresh).setVisibility(View.VISIBLE);
	}

	public static void showBack(Activity activity, int message) {
		activity.findViewById(R.id.loading).setVisibility(View.GONE);
		TextView messageView = (TextView) activity.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
		activity.findViewById(R.id.back).setVisibility(View.VISIBLE);	
	}

	public static void showEmpty(Activity activity, int message) {
		activity.findViewById(R.id.loading).setVisibility(View.GONE);
		activity.findViewById(R.id.back).setVisibility(View.GONE);
		TextView messageView = (TextView) activity.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
	}

	public static void setTitle(Activity activity, String title) {
		((TextView) activity.findViewById(R.id.title_text)).setText(title);
	}

	public static void setTitle(Activity activity, int title) {
		((TextView) activity.findViewById(R.id.title_text)).setText(title);
	}

	public static void setTitleIcon(Activity activity, int icon) {
		((ImageView) activity.findViewById(R.id.title_icon)).setImageResource(icon);
	}

	public static void setTitle(Activity activity, int title, int icon) {
		setTitle(activity, title);
		setTitleIcon(activity, icon);
	}

	public static void setTitle(Activity activity, String title, int icon) {
		setTitle(activity, title);
		setTitleIcon(activity, icon);
	}

	public static void setTitleSize(Activity activity, float size) {
		((TextView) activity.findViewById(R.id.title_text)).setTextSize(size);
	}

	public static String capitalize(String text) {
		if(text == null) 
			return "";
		if(text.length() == 0)
			return text;
		if(text.length() == 1) 
			return text.toUpperCase();
		return text.substring(0, 1).toUpperCase() + text.substring(1);
	}
	
	public static boolean hasShownFavoritesMessage(Context context) {
		return getBooleanPreference(context, "favorites_toast", false);
	}
	
	public static void markShownFavoritesMessage(Context context) {
		setBooleanPreference(context, "favorites_toast", true);
	}
	
	public static String getStringPreference(Context context, String key) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, null);
	}
	
	public static boolean setStringPreference(Context context, String key, String value) {
		return PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).commit();
	}
	
	public static boolean getBooleanPreference(Context context, String key, boolean defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
	}
	
	public static boolean setBooleanPreference(Context context, String key, boolean value) {
		return PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).commit();
	}
	
	/* 
	 * Using reflection to support custom tabs for 1.6 and up, and default to regular tabs for 1.5.
	 */
	
	static {
		checkCustomTabs();
	}
	
	// check for existence of TabHost.TabSpec#setIndicator(View)
	private static void checkCustomTabs() {
		try {
    	   setView = TabHost.TabSpec.class.getMethod("setIndicator", new Class[] { View.class } );
       } catch (NoSuchMethodException nsme) {}
	}
	
	public static void addTab(Activity activity, TabHost tabHost, String tag, String name, Intent intent, Drawable backup) {
		TabHost.TabSpec tab = tabHost.newTabSpec(tag).setContent(intent);
		if (setView != null) {
			try {
				setView.invoke(tab, tabView(activity, name));
			} catch (IllegalAccessException ie) {
				throw new RuntimeException(ie);
			} catch (InvocationTargetException ite) {
				Throwable cause = ite.getCause();
				if (cause instanceof RuntimeException)
					throw (RuntimeException) cause;
				else if (cause instanceof Error)
					throw (Error) cause;
				else
					throw new RuntimeException(ite);
			}
		} else // default 1.5 tabs
			tab.setIndicator(name, backup);
		
		tabHost.addTab(tab);
	}
	
	public static View tabView(Context context, String name) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View tab = inflater.inflate(R.layout.tab_minimal, null);
		((TextView) tab.findViewById(R.id.tab_name)).setText(name);
		return tab;
	}

	public static String districtMapUrl(String title, String state, String district) {
		String url = "http://data.politiwidgets.com/kml/";
		String session = "110";
		
		if (title.equals("Sen"))
			url += "states/" + state;
		else
			url += "cds/" + session + "/" + state + "-" + district;
		
		url += ".kml";
		
		return url;
	}
}