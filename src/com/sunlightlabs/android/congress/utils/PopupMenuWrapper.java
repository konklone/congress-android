package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.view.View;
import android.widget.PopupMenu;

public class PopupMenuWrapper {
	private PopupMenu menu;
	
	PopupMenuWrapper(Activity activity, View view) {
		menu = new PopupMenu(activity, view);
	}
	
	public void setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener listener) {
		menu.setOnMenuItemClickListener(listener);
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