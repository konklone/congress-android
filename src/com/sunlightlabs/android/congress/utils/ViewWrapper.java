package com.sunlightlabs.android.congress.utils;

import android.view.View;

public class ViewWrapper {
	private View view;
	private Object tag;
	private boolean enabled = true; // by default

	public ViewWrapper(View view) {
		this.view = view;
	}

	public ViewWrapper(View view, Object tag) {
		this(view);
		this.tag = tag;
	}

	public View getView() {
		return view;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Object getTag() {
		return tag;
	}

	public void setTag(Object tag) {
		this.tag = tag;
	}
}
