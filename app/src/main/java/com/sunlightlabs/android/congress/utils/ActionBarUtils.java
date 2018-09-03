package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.sunlightlabs.android.congress.MenuMain;
import com.sunlightlabs.android.congress.R;

public class ActionBarUtils {

	public interface HasActionMenu {
		void menuSelected(MenuItem item);
	}

	public static void setTitle(Activity activity, String title) {
		setTitle(activity, title, new Intent(activity, MenuMain.class)); // default intent is home
	}

	public static void setTitle(Activity activity, int title) {
		setTitle(activity, activity.getResources().getString(title));
	}

	public static void setTitle(Activity activity, int title, Intent up) {
		setTitle(activity, activity.getResources().getString(title), up);
	}

	public static void setTitle(Activity activity, String title, Intent up) {
		((TextView) activity.findViewById(R.id.title_text)).setText(title);
		
		if (up != null) // send a null up intent to disable up button
			setTitleButton(activity, up); 
	}

	public static void setTitleIcon(Activity activity, Drawable drawable) {
		((ImageView) activity.findViewById(R.id.title_icon)).setImageDrawable(drawable);
	}

	public static void setTitleButton(final Activity activity, final Intent up) {
		View button = activity.findViewById(R.id.title_button);

		button.findViewById(R.id.title_up_icon).setVisibility(View.VISIBLE);
		button.setOnClickListener(v -> activity.startActivity(up.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
		button.setFocusable(true);
	}

	public static void setTitleSize(Activity activity, float size) {
		((TextView) activity.findViewById(R.id.title_text)).setTextSize(size);
	}

	public static void setActionButton(Activity activity, int id, int icon, View.OnClickListener listener) {
		ViewGroup button = activity.findViewById(id);
		button.setOnClickListener(listener);
		((ImageView) button.findViewById(R.id.icon)).setImageResource(icon);
		button.setVisibility(View.VISIBLE);
	}

	public static void setActionIcon(Activity activity, int id, int icon) {
		ViewGroup button = activity.findViewById(id);
		((ImageView) button.findViewById(R.id.icon)).setImageResource(icon);
	}

	public static void setActionMenu(final Activity activity, int menuId) {
        View menuView = activity.findViewById(R.id.action_menu);
        final PopupMenu menu = new PopupMenu(activity, menuView);

        menu.inflate(menuId);
		menu.setOnMenuItemClickListener(item -> {
			((HasActionMenu) activity).menuSelected(item);
			return false;
		});

		menuView.setOnClickListener(v -> menu.show());

        menuView.setVisibility(View.VISIBLE);
	}
}