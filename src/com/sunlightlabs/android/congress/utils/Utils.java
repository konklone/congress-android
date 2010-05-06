package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sunlightlabs.android.congress.BillInfo;
import com.sunlightlabs.android.congress.LegislatorTabs;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Drumbone;
import com.sunlightlabs.congress.java.Legislator;
import com.sunlightlabs.congress.java.Sunlight;

public class Utils {
	public static void setupDrumbone(Context context) {
		Resources resources = context.getResources();
		Drumbone.userAgent = resources.getString(R.string.drumbone_user_agent);
		Drumbone.apiKey = resources.getString(R.string.sunlight_api_key);
		Drumbone.baseUrl = resources.getString(R.string.drumbone_base_url);
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
			.putExtra("lastName", legislator.last_name)
			.putExtra("state", legislator.state)
			.putExtra("party", legislator.party)
			.putExtra("gender", legislator.gender)
			.putExtra("domain", legislator.getDomain())
			.putExtra("office", legislator.congress_office)
			.putExtra("website", legislator.website)
			.putExtra("phone", legislator.phone)
			.putExtra("twitter_id", legislator.twitter_id)
			.putExtra("youtube_id", legislator.youtubeUsername());
	}
	
	// suitable for going from a list to the bill display page
	// not suitable for shortcut intents
	public static Intent billIntentExtra(Context context, Bill bill) {
		com.sunlightlabs.congress.java.Legislator sponsor = bill.sponsor;
		Intent intent = new Intent(context, BillInfo.class)
			.putExtra("extra", true)
			.putExtra("id", bill.id)
			.putExtra("code", bill.code)
			.putExtra("short_title", bill.short_title)
			.putExtra("official_title", bill.official_title)
			.putExtra("introduced_at", bill.introduced_at.getTime());
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
	
	// suitable for shortcut intents, standalone, bill ID only
	public static Intent billIntent(Context context, Bill bill) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
    	intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.BillInfo");
		intent.putExtra("extra", false).putExtra("id", bill.id);
		return intent;
	}
	
	public static Intent shortcutIntent(Context context, Bill bill) {
		Parcelable resource = Intent.ShortcutIconResource.fromContext(context, R.drawable.bill);
		return new Intent()
			.putExtra(Intent.EXTRA_SHORTCUT_INTENT, 
				billIntent(context, bill).addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
			.putExtra(Intent.EXTRA_SHORTCUT_NAME, Bill.formatCode(bill.code))
			.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, resource);
	}
	
	public static Intent shortcutIntent(Context context, Legislator legislator, Bitmap icon) {
		return shortcutIntent(context, legislator.bioguide_id, legislator.last_name, icon);
	}
	
	public static Intent shortcutIntent(Context context, String legislatorId, String name, Bitmap icon) {
		return new Intent()
			.putExtra(Intent.EXTRA_SHORTCUT_INTENT, 
					Utils.legislatorIntent(legislatorId).addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
			.putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
			.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon); 
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
}