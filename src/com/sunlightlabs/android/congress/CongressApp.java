package com.sunlightlabs.android.congress;

import android.app.Application;
import android.content.res.Resources;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class CongressApp extends Application {
    // default to true, will become false if xml is missing

    private Tracker local;
    private boolean hasLocal = true;

    private Tracker global;
    private boolean hasGlobal = true;

    // using getter pattern with sync to ensure safe, idempotent, creation of one tracker
    public synchronized Tracker appTracker() {
        if (!hasLocal) return null;

        if (local == null) {

            try {
                getResources().getXml(R.xml.analytics);
            } catch(Resources.NotFoundException e) {
                hasLocal = false;
                return null;
            }

            local = GoogleAnalytics.getInstance(this).newTracker(R.xml.analytics);
        }

        return local;
    }

    public synchronized Tracker globalTracker() {
        if (!hasGlobal) return null;

        if (global == null) {
            try {
                getResources().getXml(R.xml.analytics_global);
            } catch(Resources.NotFoundException e) {
                hasGlobal = false;
                return null;
            }

            global = GoogleAnalytics.getInstance(this).newTracker(R.xml.analytics_global);
        }

        return global;
    }
}
