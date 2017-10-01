package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

public class PopupMenuWrapper {
	private PopupMenu menu;
	
	interface OnMenuItemClickListener {
		boolean onMenuItemClick(MenuItem item);
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
		// done without using the PopupMenu.inflate() method, which isn't available on Honeycomb.
		// this method works on both Honeycomb and Ice Cream Sandwich.
		MenuInflater inflater = menu.getMenuInflater();
		Menu baseMenu = menu.getMenu();
		inflater.inflate(menuId, baseMenu);
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