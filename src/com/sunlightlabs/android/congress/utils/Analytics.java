package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.Settings;

/**
 * Helper class to manage Google Analytics tracking. All calls to log a page view or event should go through this class,
 * as it centralizes the check on whether the user has opted out of tracking altogether.
 * 
 * All calls to log a page view or an event are dispatched immediately.
 */

public class Analytics {
	
	// scope constants as used in Google Analytics
	public static final int SCOPE_VISITOR = 1;
	public static final int SCOPE_SESSION = 2;
	public static final int SCOPE_PAGE = 3;
	
	// custom variable slot info for entry tracking
	public static final int CUSTOM_ENTRY_SLOT = 1;
	public static final String CUSTOM_ENTRY_NAME = "Entry";
	
	// for use in investigating intents for entry sources
	public static final String EXTRA_ENTRY_FROM = "com.sunlightlabs.android.congress.utils.ENTRY_FROM";
	
	// types of entry into the application
	public static final String ENTRY_MAIN = "main";
	public static final String ENTRY_SHORTCUT = "shortcut";
	public static final String ENTRY_NOTIFICATION = "notification";
	
	// categories of events
	public static final String EVENT_FAVORITE_CATEGORY = "favorites";
	public static final String EVENT_NOTIFICATION_CATEGORY = "notifications";
	
	// event values
	public static final String FAVORITE_ADD_LEGISLATOR = "add_legislator";
	public static final String FAVORITE_REMOVE_LEGISLATOR = "remove_legislator";
	public static final String FAVORITE_ADD_BILL = "add_bill";
	public static final String FAVORITE_REMOVE_BILL = "remove_bill";
	public static final String NOTIFICATION_ADD = "subscribe";
	public static final String NOTIFICATION_REMOVE = "unsubscribe";
	
	
	public static GoogleAnalyticsTracker start(Activity activity) {
		GoogleAnalyticsTracker tracker = null;
		
		if (analyticsEnabled(activity)) {
			Log.i(Utils.TAG, "[Analytics] Tracker starting");
			tracker = GoogleAnalyticsTracker.getInstance();
			String code = activity.getResources().getString(R.string.google_analytics_tracking_code);
			tracker.start(code, activity);
		}
		
		return tracker;
	}
	
	public static void page(Activity activity, GoogleAnalyticsTracker tracker, String page) {
		if (tracker != null && analyticsEnabled(activity)) {
			String source = entrySource(activity);
			if (source != null) {
				Log.i(Utils.TAG, "[Analytics] Marking next page view as an entry to the app of type: " + source);
				tracker.setCustomVar(CUSTOM_ENTRY_SLOT, CUSTOM_ENTRY_NAME, source, SCOPE_SESSION);
			}
			
			Log.i(Utils.TAG, "[Analytics] Tracking page - " + page);
			tracker.trackPageView(page);
			tracker.dispatch();
		}
	}
		
	public static void event(Activity activity, GoogleAnalyticsTracker tracker, String category, String action, String label) {
		if (tracker != null && analyticsEnabled(activity)) {
			Log.i(Utils.TAG, "[Analytics] Tracking event - category: " + category + ", action: " + action + ", label: " + label);
			tracker.trackEvent(category, action, label, -1);
			tracker.dispatch();
		}
	}
	
	public static void stop(GoogleAnalyticsTracker tracker) {
		if (tracker != null) {
			Log.i(Utils.TAG, "[Analytics] Tracker stopping");
			tracker.stop();
		}
	}
	
	public static boolean analyticsEnabled(Activity activity) {
		boolean debugDisabled = activity.getResources().getString(R.string.debug_disable_analytics).equals("true");
		boolean userEnabled = Utils.getBooleanPreference(activity, Settings.ANALYTICS_ENABLED_KEY, Settings.ANALYTICS_ENABLED_DEFAULT);
		return (!debugDisabled && userEnabled);
	}
	
	public static void addFavoriteLegislator(Activity activity, GoogleAnalyticsTracker tracker, String bioguideId) {
		event(activity, tracker, EVENT_FAVORITE_CATEGORY, FAVORITE_ADD_LEGISLATOR, bioguideId);
	}
	
	public static void removeFavoriteLegislator(Activity activity, GoogleAnalyticsTracker tracker, String bioguideId) {
		event(activity, tracker, EVENT_FAVORITE_CATEGORY, FAVORITE_REMOVE_LEGISLATOR, bioguideId);
	}
	
	public static void addFavoriteBill(Activity activity, GoogleAnalyticsTracker tracker, String billId) {
		event(activity, tracker, EVENT_FAVORITE_CATEGORY, FAVORITE_ADD_BILL, billId);
	}
	
	public static void removeFavoriteBill(Activity activity, GoogleAnalyticsTracker tracker, String billId) {
		event(activity, tracker, EVENT_FAVORITE_CATEGORY, FAVORITE_REMOVE_BILL, billId);
	}
	
	public static void subscribeNotification(Activity activity, GoogleAnalyticsTracker tracker, String subscriber) {
		event(activity, tracker, EVENT_NOTIFICATION_CATEGORY, NOTIFICATION_ADD, subscriber);
	}
	
	public static void unsubscribeNotification(Activity activity, GoogleAnalyticsTracker tracker, String subscriber) {
		event(activity, tracker, EVENT_NOTIFICATION_CATEGORY, NOTIFICATION_REMOVE, subscriber);
	}
	
	/** Utility function for discerning an entry source from an activity's Intent. */ 
	public static String entrySource(Activity activity) {
		Intent intent = activity.getIntent();
		String action = intent.getAction();
		boolean main = action != null && action.equals(Intent.ACTION_MAIN);
		if (main) {
			String source = ENTRY_MAIN;
			Bundle extras = intent.getExtras();
			if (extras != null) {
				String extra = extras.getString(EXTRA_ENTRY_FROM);
				if (extra != null)
					source = extra;
			}
			return source;
		} else
			return null;
	}
	
}