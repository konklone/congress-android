package com.sunlightlabs.android.congress;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class CongressApp extends Application {
    private Tracker tracker;

    public synchronized Tracker appTracker() {
        if (tracker == null)
            tracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.tracker);

        return tracker;
    }
}
