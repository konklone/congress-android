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
	
	// constants for various custom variables
	public static final int CUSTOM_ENTRY_SLOT = 1;
	public static final String CUSTOM_ENTRY_NAME = "Entry";
	public static final String ENTRY_MAIN = "main";
	public static final String ENTRY_SHORTCUT = "shortcut";
	
	
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
	
	public static void page(GoogleAnalyticsTracker tracker, String page, String entrySource) {
		if (tracker != null && analyticsEnabled()) {
			Log.i(Utils.TAG, "[Analytics] Marking next page view as an entry to the app of type: " + entrySource);
			tracker.setCustomVar(CUSTOM_ENTRY_SLOT, CUSTOM_ENTRY_NAME, entrySource, SCOPE_SESSION);
			page(tracker, page);
		}
	}
	
	public static void page(GoogleAnalyticsTracker tracker, String page) {
		if (tracker != null && analyticsEnabled()) {
			Log.i(Utils.TAG, "[Analytics] Tracking page - " + page);
			tracker.trackPageView(page);
			tracker.dispatch();
		}
	}
		
	public static void event(GoogleAnalyticsTracker tracker, String category, String action, String label, int value) {
		if (tracker != null && analyticsEnabled()) {
			Log.i(Utils.TAG, "[Analytics] Tracking event - category: " + category + ", action: " + action + ", label: " + label + ", value: " + value);
			tracker.trackEvent(category, action, label, value);
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
	
}
