package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.sunlightlabs.android.congress.BillPager;
import com.sunlightlabs.android.congress.LegislatorPager;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.RollInfo;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.Congress;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Utils {
	public static final String TAG = "Congress";
	
	public static SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm aa");
	
	public static void setupAPI(Context context) {
		Resources resources = context.getResources();
		
		Congress.baseUrl = resources.getString(R.string.api_endpoint);
		Congress.userAgent = resources.getString(R.string.api_user_agent);
		Congress.apiKey = resources.getString(R.string.sunlight_api_key);
		Congress.appVersion = resources.getString(R.string.app_version);
		Congress.appChannel = resources.getString(R.string.market_channel);
		
		Congress.osVersion = "Android " + Build.VERSION.SDK_INT;
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
	
	public static String formatRollId(String id) {
		Roll tempRoll = Roll.splitRollId(id);
		return Utils.capitalize(tempRoll.chamber) + " Roll No. " + tempRoll.number;
	}
	
	public static Intent legislatorIntent(String id) {
		return new Intent().setClassName(
				"com.sunlightlabs.android.congress",
				"com.sunlightlabs.android.congress.LegislatorPager")
			.putExtra("bioguide_id", id);
	}

	public static Intent legislatorIntent(Context context, Legislator legislator) {
		return new Intent(context, LegislatorPager.class)
			.putExtra("bioguide_id", legislator.bioguide_id)
			.putExtra("legislator", legislator);
	}

   public static Intent legislatorLoadIntent(String id, Intent intent) {
	   return new Intent().setClassName(
			   "com.sunlightlabs.android.congress",
			   "com.sunlightlabs.android.congress.LegislatorLoader")
		   .putExtra("id", id)
		   .putExtra("intent", intent);
}


	public static Intent billIntent(Context context, Bill bill) {
		return new Intent(context, BillPager.class)
			.putExtra("bill_id", bill.id)
			.putExtra("bill", bill);
	}
	
	public static Intent billIntent(String billId) {
		return new Intent().setClassName(
				"com.sunlightlabs.android.congress",
				"com.sunlightlabs.android.congress.BillPager")
			.putExtra("bill_id", billId);
	}
	
	public static Intent rollIntent(Context context, Roll roll) {
		return new Intent(context, RollInfo.class)
			.putExtra("id", roll.id)
			.putExtra("roll", roll);
	}
	
	public static Intent rollIntent(Context context, String rollId) {
		return new Intent(context, RollInfo.class)
			.putExtra("id", rollId);
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

	public static String truncate(String text, int length, boolean ellipses) {
		if (text.length() > length)
			return text.substring(0, length - 3) + (ellipses ? "..." : "");
		else
			return text;
	}
	
	public static String truncate(String text, int length) {
		return truncate(text, length, true);
	}


	/** TODO: Remove these list-oriented show methods entirely once we're all on ListFragments */
	
	public static void showLoading(Activity activity) {
		activity.findViewById(R.id.empty_message).setVisibility(View.GONE);
		activity.findViewById(R.id.refresh).setVisibility(View.GONE);
		activity.findViewById(R.id.loading).setVisibility(View.VISIBLE);
	}
	
	public static void hideList(Activity activity) {
		activity.findViewById(android.R.id.empty).setVisibility(View.GONE);
	}

	public static void setLoading(Activity activity, int message) {
		((TextView) activity.findViewById(R.id.loading_message)).setText(message);
	}
	
	public static void showBack(Activity activity, int message) {
		showBack(activity, activity.getResources().getString(message));
	}
	
	public static void showEmpty(Activity activity, int message) {
		showEmpty(activity, activity.getResources().getString(message));
	}
	
	public static void showRefresh(Activity activity, int message) {
		showRefresh(activity, activity.getResources().getString(message));
	}

	public static void showRefresh(Activity activity, String message) {
		activity.findViewById(R.id.loading).setVisibility(View.GONE);
		TextView messageView = (TextView) activity.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
		activity.findViewById(R.id.refresh).setVisibility(View.VISIBLE);
	}

	public static void showBack(Activity activity, String message) {
		activity.findViewById(R.id.loading).setVisibility(View.GONE);
		TextView messageView = (TextView) activity.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
		activity.findViewById(R.id.back).setVisibility(View.VISIBLE);	
	}

	public static void showEmpty(Activity activity, String message) {
		activity.findViewById(R.id.loading).setVisibility(View.GONE);
		activity.findViewById(R.id.back).setVisibility(View.GONE);
		TextView messageView = (TextView) activity.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
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
	
	public static String getStringPreference(Context context, String key, String value) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, value);
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
	
	
	public static void addTab(Activity activity, TabHost tabHost, String tag, Intent intent, int name) {
		tabHost.addTab(tabHost.newTabSpec(tag).setContent(intent).setIndicator(tabView(activity, name)));
	}
	
	public static View tabView(Context context, int name) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View tab = inflater.inflate(R.layout.tab_1, null);
		((TextView) tab.findViewById(R.id.tab_name)).setText(name);
		return tab;
	}
	
	public static final String START_NOTIFICATION_SERVICE = "com.sunlightlabs.android.congress.intent.action.START_NOTIFICATION_SERVICE";
	public static final String STOP_NOTIFICATION_SERVICE = "com.sunlightlabs.android.congress.intent.action.STOP_NOTIFICATION_SERVICE";

	public static void startNotificationsBroadcast(Context context) {
		context.sendBroadcast(new Intent(START_NOTIFICATION_SERVICE));
	}

	public static void stopNotificationsBroadcast(Context context) {
		context.sendBroadcast(new Intent(STOP_NOTIFICATION_SERVICE));
	}
	
	public static String nearbyDate(Date subject) {
		SimpleDateFormat testFormat = new SimpleDateFormat("yyyy-MM-dd");
		String today = testFormat.format(Calendar.getInstance().getTime());
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, 1);
		String tomorrow = testFormat.format(cal.getTime());
		cal.add(Calendar.DAY_OF_MONTH, -2);
		String yesterday = testFormat.format(cal.getTime());
		
		String subjectDate = testFormat.format(subject);
		
		// SimpleDateFormat dateNameFormat = new SimpleDateFormat("EEEE");
		
		if (today.equals(subjectDate))
			return "TODAY";
		else if (tomorrow.equals(subjectDate))
			return "TOMORROW"; 
		else if (yesterday.equals(subjectDate))
			return "YESTERDAY";
		else
			return null;
	}
	
	public static String fullDate(Date subject) {
		return new SimpleDateFormat("MMM d").format(subject).toUpperCase();
	}
	
	public static String nearbyOrFullDate(Date subject) {
		String result = nearbyDate(subject);
		if (result != null)
			return result;
		else
			return fullDateThisYear(subject);
	}
	
	public static String fullDateThisYear(Date subject) {
		SimpleDateFormat otherYearFormat = new SimpleDateFormat("MMM d, yyyy");
		SimpleDateFormat thisYearFormat = new SimpleDateFormat("MMM d");
		int thisYear = new GregorianCalendar().get(Calendar.YEAR);
		
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(subject);
		int year = calendar.get(Calendar.YEAR);
		
		return ((year == thisYear) ? thisYearFormat : otherYearFormat).format(subject).toUpperCase();
	}
	
	public static View dateView(Context context, Date subject, String fullDate) {
		View view = LayoutInflater.from(context).inflate(R.layout.list_item_date, null);
		
		String nearbyDate = Utils.nearbyDate(subject);
		
		TextView nearby = (TextView) view.findViewById(R.id.date_left);
		TextView full = (TextView) view.findViewById(R.id.date_right);
		if (nearbyDate != null) {
			nearby.setText(nearbyDate);
			full.setText(fullDate);
		} else {
			nearby.setText(fullDate);
			full.setVisibility(View.GONE);
		}
		
		return view;
	}
	
	public static View dateTimeView(Context context, Date subject) {
		View view = LayoutInflater.from(context).inflate(R.layout.list_item_date, null);
		
		((TextView) view.findViewById(R.id.date_left)).setText(Utils.nearbyOrFullDate(subject));
		((TextView) view.findViewById(R.id.date_right)).setText(timeFormat.format(subject));
		
		return view;
	}
		
}