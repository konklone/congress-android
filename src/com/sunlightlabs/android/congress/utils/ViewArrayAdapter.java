package com.sunlightlabs.android.congress.utils;

import java.util.ArrayList;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * Dirt simple class, to give views the selectable appearance on click and long click
 * that one expects from listviews, but made to be easily dumped into a MergeAdapter
 * for use in the middle of a large scrollable pane.
 */
public class ViewArrayAdapter extends ArrayAdapter<View> {

    public ViewArrayAdapter(Activity context, ArrayList<View> items) {
        super(context, 0, items);
    }

	public View getView(int position, View convertView, ViewGroup parent) {
		return getItem(position);
	}
}