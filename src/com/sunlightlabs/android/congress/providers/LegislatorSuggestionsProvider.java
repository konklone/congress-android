package com.sunlightlabs.android.congress.providers;

import android.content.SearchRecentSuggestionsProvider;

public class LegislatorSuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.sunlightlabs.android.congress.providers.LegislatorSuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public LegislatorSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}