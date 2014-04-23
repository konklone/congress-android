package com.sunlightlabs.android.congress;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class CongressApp extends Application {
    private Tracker app;
    private Tracker global; // optional roll-up tracker

    // using getter pattern with sync to ensure safe, idempotent, creation of one tracker
    public synchronized Tracker appTracker() {
        if (app == null)
            app = GoogleAnalytics.getInstance(this).newTracker(R.xml.analytics);

        return app;
    }

    public synchronized Tracker globalTracker() {
        if (global == null)
            global = GoogleAnalytics.getInstance(this).newTracker(R.xml.analytics_global);

        return global;
    }
}
