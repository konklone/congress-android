package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import com.sunlightlabs.android.congress.AlertFragment;
import com.sunlightlabs.android.congress.R;

public class FragmentUtils {
	
	public static void alertDialog(FragmentActivity activity, int type) {
		AlertFragment.create(type).show(activity.getSupportFragmentManager(), "dialog");
	}
	
	public static void showLoading(Fragment fragment) {
		Activity activity = fragment.getActivity();
		activity.findViewById(R.id.empty_message).setVisibility(View.GONE);
		activity.findViewById(R.id.refresh).setVisibility(View.GONE);
		activity.findViewById(R.id.loading).setVisibility(View.VISIBLE);
	}

	public static void setLoading(Fragment fragment, int message) {
		Activity activity = fragment.getActivity();
		((TextView) activity.findViewById(R.id.loading_message)).setText(message);
	}
	
	public static void showBack(Fragment fragment, int message) {
		Activity activity = fragment.getActivity();
		Utils.showBack(activity, activity.getResources().getString(message));
	}
	
	public static void showEmpty(Fragment fragment, int message) {
		Activity activity = fragment.getActivity();
		Utils.showEmpty(activity, activity.getResources().getString(message));
	}
	
	public static void showRefresh(Fragment fragment, int message) {
		Activity activity = fragment.getActivity();
		Utils.showRefresh(activity, activity.getResources().getString(message));
	}

	public static void showRefresh(Fragment fragment, String message) {
		Activity activity = fragment.getActivity();
		activity.findViewById(R.id.loading).setVisibility(View.GONE);
		TextView messageView = (TextView) activity.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
		activity.findViewById(R.id.refresh).setVisibility(View.VISIBLE);
	}

	public static void showBack(Fragment fragment, String message) {
		Activity activity = fragment.getActivity();
		activity.findViewById(R.id.loading).setVisibility(View.GONE);
		TextView messageView = (TextView) activity.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
		activity.findViewById(R.id.back).setVisibility(View.VISIBLE);	
	}

	public static void showEmpty(Fragment fragment, String message) {
		Activity activity = fragment.getActivity();
		activity.findViewById(R.id.loading).setVisibility(View.GONE);
		activity.findViewById(R.id.back).setVisibility(View.GONE);
		TextView messageView = (TextView) activity.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
	}
	
}
