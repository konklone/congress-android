package com.sunlightlabs.android.congress.utils;

import android.view.View;

public class ViewWrapper {
	public View view;
	public Object tag;
	public boolean enabled = true; // by default

	public ViewWrapper(View view) {
		this.view = view;
	}

	public ViewWrapper(View view, Object tag) {
		this(view);
		this.tag = tag;
	}
}