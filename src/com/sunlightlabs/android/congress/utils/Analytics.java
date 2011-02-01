package com.sunlightlabs.android.congress.utils;

import android.content.Context;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.R;

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
	
	
	public static GoogleAnalyticsTracker start(Context context) {
		GoogleAnalyticsTracker tracker = null;
		
		if (analyticsEnabled()) {
			Log.i(Utils.TAG, "[Analytics] Tracker starting");
			tracker = GoogleAnalyticsTracker.getInstance();
			String code = context.getResources().getString(R.string.google_analytics_tracking_code);
			tracker.start(code, context);
		}
		
		return tracker;
	}
	
	public static void page(GoogleAnalyticsTracker tracker, String page) {
		if (tracker != null && analyticsEnabled()) {
			Log.i(Utils.TAG, "[Analytics] Tracking page - " + page);
			tracker.trackPageView(page);
			tracker.dispatch();
		}
	}
		
	public static void event(GoogleAnalyticsTracker tracker, String category, String action, String label) {
		if (tracker != null && analyticsEnabled()) {
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
	
	public static boolean analyticsEnabled() {
		//TODO: check flag in keys.xml, and user settings flag
		return true;
	}
	
	/** Track a pageview, and mark it as an "entry" into the app, with the source provided. */ 
	public static void page(GoogleAnalyticsTracker tracker, String page, String source) {
		if (tracker != null && analyticsEnabled()) {
			Log.i(Utils.TAG, "[Analytics] Marking next page view as an entry to the app of type: " + source);
			tracker.setCustomVar(CUSTOM_ENTRY_SLOT, CUSTOM_ENTRY_NAME, source, SCOPE_SESSION);
			page(tracker, page);
		}
	}
	
	public static void addFavoriteLegislator(GoogleAnalyticsTracker tracker, String bioguideId) {
		event(tracker, EVENT_FAVORITE_CATEGORY, FAVORITE_ADD_LEGISLATOR, bioguideId);
	}
	
	public static void removeFavoriteLegislator(GoogleAnalyticsTracker tracker, String bioguideId) {
		event(tracker, EVENT_FAVORITE_CATEGORY, FAVORITE_REMOVE_LEGISLATOR, bioguideId);
	}
	
	public static void addFavoriteBill(GoogleAnalyticsTracker tracker, String billId) {
		event(tracker, EVENT_FAVORITE_CATEGORY, FAVORITE_ADD_BILL, billId);
	}
	
	public static void removeFavoriteBill(GoogleAnalyticsTracker tracker, String billId) {
		event(tracker, EVENT_FAVORITE_CATEGORY, FAVORITE_REMOVE_BILL, billId);
	}
	
}
