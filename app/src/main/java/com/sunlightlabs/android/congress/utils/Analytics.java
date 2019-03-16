package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.sunlightlabs.android.congress.CongressApp;
import com.sunlightlabs.android.congress.NotificationSettings;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.Settings;

import java.util.Map;

/**
 * Helper class to manage Google Analytics tracking. 
 * All calls to log an activity visit or event should go through this class,
 * as it centralizes the check on whether the user has opted out of tracking altogether.
 */

public class Analytics {

    // in onCreate(), ensure trackers are created and configured for that activity
    public static void init(Activity activity) {
        if (analyticsEnabled(activity))
            ((CongressApp) activity.getApplication()).appTracker();
    }

    // in onStart(), start auto-tracking with any previously initialized trackers
    public static void start(Activity activity) {
        if (analyticsEnabled(activity)) {
            Log.i(Utils.TAG, "[Analytics] Tracker starting for " + activity.getLocalClassName());
            GoogleAnalytics.getInstance(activity).reportActivityStart(activity);

            // send an event to insist that custom dimensions get associated with other activity.
            // wasteful, but this is done because there is no longer a way to set custom dimensions
            // at a tracker level, while using auto-tracking for activities.
            ping(activity);
        }
    }

    // in onStop(), stop auto-tracking with any previously initialized trackers
    public static void stop(Activity activity) {
        Log.i(Utils.TAG, "[Analytics] Tracker stopping for " + activity.getLocalClassName());
        GoogleAnalytics.getInstance(activity).reportActivityStop(activity);
    }

    // set the Google opt-out mid-stream
    public static void optout(Activity activity, boolean value) {
        Log.i(Utils.TAG, "[Analytics] Setting app-wide GA opt-out to: " + value);
        GoogleAnalytics.getInstance(activity).setAppOptOut(value);
    }


    public static void event(Activity activity, String category, String action, String label) {
		if (analyticsEnabled(activity)) {

            if (label == null) label = "";

			Log.i(Utils.TAG, "[Analytics] Tracking event - category: " + category + ", action: " + action + ", label: " + label);

            Map<String,String> event = attachCustomDimensions(activity, new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
            ).build();

            ((CongressApp) activity.getApplication()).appTracker().send(event);
		}
	}
	
	public static boolean analyticsEnabled(Activity activity) {
		boolean debugDisabled = activity.getResources().getString(R.string.debug_disable_analytics).equals("true");

        // these should be in sync, but in case they get out of sync, err towards turning off analytics
        boolean googleOptout = GoogleAnalytics.getInstance(activity).getAppOptOut();
		boolean userEnabled = Utils.getBooleanPreference(activity, Settings.ANALYTICS_ENABLED_KEY, Settings.ANALYTICS_ENABLED_DEFAULT);

		return (!debugDisabled && !googleOptout && userEnabled);
	}
	
	
	/*
	 *  Custom dimensions and metrics.
	 */
	
	public static final int DIMENSION_NOTIFICATIONS_ON = 3; // whether notifications are enabled (session)
	
	public static final int DIMENSION_ENTRY = 4; // how the user entered the app (hit)
	
	public static HitBuilders.EventBuilder attachCustomDimensions(Activity activity, HitBuilders.EventBuilder event) {
		Resources res = activity.getResources();

		boolean notificationsOn = Utils.getBooleanPreference(activity, NotificationSettings.KEY_NOTIFY_ENABLED, false);
        event = event.setCustomDimension(DIMENSION_NOTIFICATIONS_ON, notificationsOn ? "on" : "off");

		String entrySource = entrySource(activity);
		if (entrySource != null)
            event = event.setCustomDimension(DIMENSION_ENTRY, entrySource);

        return event;
	}
	
	
	/*
	 *  Utility function for discerning an entry source from an activity's Intent. 
	 */
	
	public static final String EXTRA_ENTRY_FROM = "com.sunlightlabs.android.congress.utils.ENTRY_FROM";
	
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
	
	public static Intent passEntry(Activity activity, Intent intent) {
		String action = activity.getIntent().getAction();
		if (action != null && action.equals(Intent.ACTION_MAIN)) {
			intent.setAction(Intent.ACTION_MAIN);
			intent.putExtra(Analytics.EXTRA_ENTRY_FROM, activity.getIntent().getStringExtra(Analytics.EXTRA_ENTRY_FROM));
		}
		
		return intent;
	}
	
	/*
	 * Event definitions
	 */
	
	// types of entry into the application
	public static final String ENTRY_MAIN = "main";
	public static final String ENTRY_NOTIFICATION = "notification";


	// categories of events
	public static final String EVENT_FAVORITE = "favorites";
	public static final String EVENT_NOTIFICATION = "notifications";
	public static final String EVENT_LEGISLATOR = "legislator";
	public static final String EVENT_BILL = "bill";
	public static final String EVENT_ANALYTICS = "analytics";
	public static final String EVENT_ABOUT = "about";
	public static final String EVENT_CHANGELOG = "changelog";
	public static final String EVENT_REVIEW = "review"; // values will be "google" or "amazon"
	public static final String EVENT_PING = "ping";

	// event values
	public static final String FAVORITE_ADD_LEGISLATOR = "add_legislator";
	public static final String FAVORITE_REMOVE_LEGISLATOR = "remove_legislator";
	public static final String FAVORITE_ADD_BILL = "add_bill";
	public static final String FAVORITE_REMOVE_BILL = "remove_bill";
	public static final String NOTIFICATION_ADD = "subscribe";
	public static final String NOTIFICATION_REMOVE = "unsubscribe";
	public static final String LEGISLATOR_CALL = "call";
	public static final String LEGISLATOR_WEBSITE = "website";
	public static final String LEGISLATOR_TWITTER = "twitter";
	public static final String LEGISLATOR_YOUTUBE = "youtube";
	public static final String LEGISLATOR_FACEBOOK = "facebook";
	public static final String LEGISLATOR_CONTACTS = "contacts";
	public static final String BILL_TEXT = "text";
	public static final String BILL_GOVTRACK = "govtrack";
	public static final String BILL_UPCOMING = "upcoming";
	public static final String ANALYTICS_DISABLE = "disable";
	public static final String ABOUT_VALUE = "open";
	public static final String CHANGELOG_VALUE = "open";
	public static final String PING_VALUE = "ping";

    public static void ping(Activity activity) {
        event(activity, EVENT_PING, PING_VALUE, null);
    }

	public static void aboutPage(Activity activity) {
		event(activity, EVENT_ABOUT, ABOUT_VALUE, null);
	}
	
	public static void changelog(Activity activity) {
		event(activity, EVENT_CHANGELOG, CHANGELOG_VALUE, null);
	}
	
	public static void reviewClick(Activity activity) {
		event(activity, EVENT_REVIEW, null, null);
	}

	public static void addFavoriteLegislator(Activity activity, String bioguideId) {
		event(activity, EVENT_FAVORITE, FAVORITE_ADD_LEGISLATOR, bioguideId);
	}
	
	public static void removeFavoriteLegislator(Activity activity, String bioguideId) {
		event(activity, EVENT_FAVORITE, FAVORITE_REMOVE_LEGISLATOR, bioguideId);
	}
	
	public static void addFavoriteBill(Activity activity, String billId) {
		event(activity, EVENT_FAVORITE, FAVORITE_ADD_BILL, billId);
	}
	
	public static void removeFavoriteBill(Activity activity, String billId) {
		event(activity, EVENT_FAVORITE, FAVORITE_REMOVE_BILL, billId);
	}
	
	public static void subscribeNotification(Activity activity, String subscriber) {
		event(activity, EVENT_NOTIFICATION, NOTIFICATION_ADD, subscriber);
	}
	
	public static void unsubscribeNotification(Activity activity, String subscriber) {
		event(activity, EVENT_NOTIFICATION, NOTIFICATION_REMOVE, subscriber);
	}
	
	public static void legislatorCall(Activity activity, String bioguideId) {
		event(activity, EVENT_LEGISLATOR, LEGISLATOR_CALL, bioguideId);
	}
	
	public static void legislatorWebsite(Activity activity, String bioguideId, String network) {
		event(activity, EVENT_LEGISLATOR, network, bioguideId);
	}
	
	public static void legislatorContacts(Activity activity, String bioguideId) {
		event(activity, EVENT_LEGISLATOR, LEGISLATOR_CONTACTS, bioguideId);
	}
	
	public static void billText(Activity activity, String billId) {
		event(activity, EVENT_BILL, BILL_TEXT, billId);
	}
	
	public static void billGovTrack(Activity activity, String billId) {
		event(activity, EVENT_BILL, BILL_GOVTRACK, billId);
	}
	
	public static void billUpcoming(Activity activity, String billId) {
		event(activity, EVENT_BILL, BILL_UPCOMING, billId);
	}
	
	public static void analyticsDisable(Activity activity) {
		event(activity, EVENT_ANALYTICS, ANALYTICS_DISABLE, "");
	}
	
}