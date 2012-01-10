package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

public class PopupMenuWrapper {
	private PopupMenu menu;
	
	interface OnMenuItemClickListener {
		public boolean onMenuItemClick(MenuItem item);
	}
	
	PopupMenuWrapper(Activity activity, View view) {
		menu = new PopupMenu(activity, view);
	}
	
	public void setOnMenuItemClickListener(final PopupMenuWrapper.OnMenuItemClickListener listener) {
		menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				listener.onMenuItemClick(item);
				return false;
			}
		});
	}
	
	public void inflate(int menuId) {
		menu.inflate(menuId);			
	}
	
	public void show() {
		menu.show();
	}
	
	static {
		try {
			Class.forName("android.widget.PopupMenu");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static void checkAvailable() {}
}