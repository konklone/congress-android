package com.sunlightlabs.android.congress.utils;

import android.app.Activity;
import android.app.Fragment;
import android.view.View;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.fragments.AlertFragment;
import com.sunlightlabs.android.congress.notifications.NotificationService;

import java.util.Arrays;
import java.util.List;

public class FragmentUtils {
	public static void setupAPI(Fragment fragment) {
		Utils.setupAPI(fragment.getActivity());
	}

	public static void alertDialog(Activity activity, int type) {
		AlertFragment.create(type).show(activity.getFragmentManager(), "dialog");
	}

	public static List<String> newIds(Fragment fragment, String subscriberClass) {
		Activity activity = fragment.getActivity();
		if (activity == null) return null;
		String[] ids = activity.getIntent().getStringArrayExtra(NotificationService.EXTRA_NEW_IDS_PREFIX + subscriberClass);
		if (ids == null) return null;
		return Arrays.asList(ids);
	}

	public static void showLoading(Fragment fragment) {
		View view = fragment.getView();
		view.findViewById(R.id.empty_message).setVisibility(View.GONE);
		view.findViewById(R.id.refresh).setVisibility(View.GONE);
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE);
	}

	public static void showRefresh(Fragment fragment, String message) {
		View view = fragment.getView();
		view.findViewById(R.id.loading).setVisibility(View.GONE);
		TextView messageView = view.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
		view.findViewById(R.id.refresh).setVisibility(View.VISIBLE);
	}

	public static void showEmpty(Fragment fragment, String message) {
		View view = fragment.getView();
		view.findViewById(R.id.loading).setVisibility(View.GONE);
		view.findViewById(R.id.back).setVisibility(View.GONE);
		TextView messageView = view.findViewById(R.id.empty_message);
		messageView.setText(message);
		messageView.setVisibility(View.VISIBLE);
	}

	public static void setLoading(Fragment fragment, int message) {
		((TextView) fragment.getView().findViewById(R.id.loading_message)).setText(message);
	}

	public static void showEmpty(Fragment fragment, int message) {
		if (fragment.getActivity() != null)
			FragmentUtils.showEmpty(fragment, fragment.getActivity().getResources().getString(message));
	}

	public static void showRefresh(Fragment fragment, int message) {
		if (fragment.getActivity() != null)
			FragmentUtils.showRefresh(fragment, fragment.getActivity().getResources().getString(message));
	}
}