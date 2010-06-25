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

	// Suitable for a direct link to a legislator, bypassing the LegislatorLoader entirely
	public static Intent legislatorIntent(Context context, Legislator legislator) {
		return new Intent(context, LegislatorTabs.class)
		.putExtra("id", legislator.bioguide_id)
		.putExtra("titledName", legislator.titledName())
		.putExtra("title", legislator.title)
		.putExtra("lastName", legislator.last_name)
		.putExtra("firstName", legislator.first_name)
		.putExtra("nickname", legislator.nickname)
		.putExtra("nameSuffix", legislator.name_suffix)
		.putExtra("title", legislator.title)
		.putExtra("state", legislator.state)
		.putExtra("party", legislator.party)
		.putExtra("gender", legislator.gender)
		.putExtra("domain", legislator.getDomain())
		.putExtra("district", legislator.district)
		.putExtra("office", legislator.congress_office)
		.putExtra("website", legislator.website)
		.putExtra("phone", legislator.phone)
		.putExtra("twitter_id", legislator.twitter_id)
		.putExtra("youtube_id", legislator.youtubeUsername())
		.putExtra("bioguide_id", legislator.bioguide_id)
		.putExtra("govtrack_id", legislator.govtrack_id);
	}

	// Suitable for going from a list to the bill display page.
	// Assumes the "basic" section of a bill has been loaded.
	// Not suitable for shortcut intents.

	// so tedious that I want a method to load up an Intent for any class
	public static Intent billIntent(Context context, Bill bill) {
		Legislator sponsor = bill.sponsor;
		Intent intent = new Intent(context, BillTabs.class)
		.putExtra("id", bill.id)
		.putExtra("type", bill.type)
		.putExtra("number", bill.number)
		.putExtra("session", bill.session)
		.putExtra("code", bill.code)
		.putExtra("short_title", bill.short_title)
		.putExtra("official_title", bill.official_title)
		.putExtra("house_result", bill.house_result)
		.putExtra("senate_result", bill.senate_result)
		.putExtra("passed", bill.passed)
		.putExtra("vetoed", bill.vetoed)
		.putExtra("override_house_result", bill.override_house_result)
		.putExtra("override_senate_result", bill.override_senate_result)
		.putExtra("awaiting_signature", bill.awaiting_signature)
		.putExtra("enacted", bill.enacted);

		if (bill.last_vote_at != null)
			intent.putExtra("last_vote_at", bill.last_vote_at.getTime());
		if (bill.last_action_at != null)
			intent.putExtra("last_action_at", bill.last_action_at.getTime());

		if (bill.introduced_at != null)
			intent.putExtra("introduced_at", bill.introduced_at.getTime());
		if (bill.house_result_at != null)
			intent.putExtra("house_result_at", bill.house_result_at.getTime());
		if (bill.senate_result_at != null)
			intent.putExtra("senate_result_at", bill.senate_result_at.getTime());
		if (bill.passed_at != null)
			intent.putExtra("passed_at", bill.passed_at.getTime());
		if (bill.vetoed_at != null)
			intent.putExtra("vetoed_at", bill.vetoed_at.getTime());
		if (bill.override_house_result_at != null)
			intent.putExtra("override_house_result_at", bill.override_house_result_at.getTime());
		if (bill.override_senate_result_at != null)
			intent.putExtra("override_senate_result_at", bill.override_senate_result_at.getTime());
		if (bill.awaiting_signature_since != null)
			intent.putExtra("awaiting_signature_since", bill.awaiting_signature_since.getTime());
		if (bill.enacted_at != null)
			intent.putExtra("enacted_at", bill.enacted_at.getTime());

		if (sponsor != null) {
			intent.putExtra("sponsor_id", sponsor.bioguide_id)
			.putExtra("sponsor_party", sponsor.party)
			.putExtra("sponsor_state", sponsor.state)
			.putExtra("sponsor_title", sponsor.title)
			.putExtra("sponsor_first_name", sponsor.first_name)
			.putExtra("sponsor_nickname", sponsor.nickname)
			.putExtra("sponsor_last_name", sponsor.last_name);
		}

		return intent;
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