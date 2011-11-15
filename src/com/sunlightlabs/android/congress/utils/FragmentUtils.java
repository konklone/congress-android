package com.sunlightlabs.android.congress.utils;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import com.sunlightlabs.android.congress.AlertFragment;
import com.sunlightlabs.android.congress.R;

public class FragmentUtils {
	
	public static void setupSunlight(Fragment fragment) {
		Utils.setupSunlight(fragment.getActivity());
	}
	
	public static void setupRTC(Fragment fragment) {
		Utils.setupRTC(fragment.getActivity());
	}
	
	public static void alertDialog(FragmentActivity activity, int type) {
		AlertFragment.create(type).show(activity.getSupportFragmentManager(), "dialog");
	}
	

	public static void showLoading(Fragment fragment) {
		View view = fragment.getView();
		view.findViewById(R.id.empty_message).setVisibility(View.GONE);
		view.findViewById(R.id.refresh).setVisibility(View.GONE);
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE);
	}
	
	public static void showBack(Fragment fragment, String message) {
		View view = fragment.getView();
		view.findViewById(R.id.loading).setVisibility(View.GONE);
		TextView messageView = (TextView) view.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
		view.findViewById(R.id.back).setVisibility(View.VISIBLE);	
	}

	public static void showRefresh(Fragment fragment, String message) {
		View view = fragment.getView();
		view.findViewById(R.id.loading).setVisibility(View.GONE);
		TextView messageView = (TextView) view.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
		view.findViewById(R.id.refresh).setVisibility(View.VISIBLE);
	}
	
	public static void showEmpty(Fragment fragment, String message) {
		View view = fragment.getView();
		view.findViewById(R.id.loading).setVisibility(View.GONE);
		view.findViewById(R.id.back).setVisibility(View.GONE);
		TextView messageView = (TextView) view.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
	}
	
	public static void setLoading(Fragment fragment, int message) {
		((TextView) fragment.getView().findViewById(R.id.loading_message)).setText(message);
	}
	
	
	public static void showBack(Fragment fragment, int message) {
		FragmentUtils.showBack(fragment, fragment.getActivity().getResources().getString(message));
	}
	
	public static void showEmpty(Fragment fragment, int message) {
		FragmentUtils.showEmpty(fragment, fragment.getActivity().getResources().getString(message));
	}
	
	public static void showRefresh(Fragment fragment, int message) {
		FragmentUtils.showRefresh(fragment, fragment.getActivity().getResources().getString(message));
	}
	
}
