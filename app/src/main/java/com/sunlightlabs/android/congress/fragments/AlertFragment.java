package com.sunlightlabs.android.congress.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class AlertFragment extends DialogFragment {

	public static final int ABOUT = 1;
	public static final int CHANGELOG = 2;
	public static final int FIRST = 3;

	public static AlertFragment create(int type) {
		AlertFragment fragment = new AlertFragment();
		Bundle args = new Bundle();
		args.putInt("type", type);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		int type = getArguments().getInt("type");

		if (type == ABOUT)
			return about(inflater);
		else if (type == CHANGELOG)
			return changelog(inflater);
		else if (type == FIRST)
			return firstTime(inflater);
		else
			return null;
	}

	public Dialog firstTime(LayoutInflater inflater) {
		View firstView = inflater.inflate(R.layout.first_time, null);

		return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.icon)
			.setTitle(R.string.app_name)
			.setView(firstView)
				.setPositiveButton(R.string.first_button, (dialog, which) -> {
				})
			.create();
	}

	public Dialog about(LayoutInflater inflater) {
		View aboutView = inflater.inflate(R.layout.about, null);

		Spanned about2 = Html.fromHtml(
            "The Congress app is developed by <a href=\"https://twitter.com/konklone\">Eric Mill</a>, a private citizen.<br/><br/>" +
            "Until 2017, development was supported by the <a href=\"https://sunlightfoundation.com\">Sunlight Foundation</a>, " +
            "a non-partisan non-profit dedicated to making government and politics more accountable and transparent.<br/><br/>" +
            "This app is powered by the <a href=\"https://www.propublica.org/datastore/api/propublica-congress-api\">Pro Publica Congress API</a>, " +
            "a service of <a href=\"https://www.propublica.org\">Pro Publica</a>, an independent, nonprofit newsroom that produces investigative journalism in the public interest."
		);
		TextView aboutView2 = aboutView.findViewById(R.id.about_2);
		aboutView2.setText(about2);
		aboutView2.setMovementMethod(LinkMovementMethod.getInstance());

        // make the Pro Publica logo clickable
        aboutView.findViewById(R.id.propublica);
		aboutView.setOnClickListener(v -> {
			Log.d(Utils.TAG, "Opening Pro Publica homepage...");
			String uri = "https://www.propublica.org";
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
		});

		return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.icon)
			.setView(aboutView)
				.setPositiveButton(R.string.about_button, (dialog, which) -> {
				})
			.create();
	}

	public Dialog changelog(LayoutInflater inflater) {
		View changelogView = inflater.inflate(R.layout.changelog, null);

		Spanned changelog = getChangelogHtml(R.array.changelog);
		Spanned changelogLast = getChangelogHtml(R.array.changelogLast);

		((TextView) changelogView.findViewById(R.id.changelog)).setText(changelog);
		((TextView) changelogView.findViewById(R.id.changelog_last_title)).setText(R.string.app_version_older);
		((TextView) changelogView.findViewById(R.id.changelog_last)).setText(changelogLast);

		ViewGroup title = (ViewGroup) inflater.inflate(R.layout.alert_dialog_title, null);
		TextView titleText = title.findViewById(R.id.title);
		titleText.setText(getResources().getString(R.string.changelog_title_prefix) + " " + getResources().getString(R.string.app_version));
		
		return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.icon)
			.setCustomTitle(title)
			.setView(changelogView)
				.setPositiveButton(R.string.changelog_button, (dialog, which) -> {
				})
			.create();
	}

	private Spanned getChangelogHtml(int stringArrayId) {
		String[] array = getActivity().getResources().getStringArray(stringArrayId);
		List<String> items = new ArrayList<>();
		for (String item : array)
			items.add("<b>&#183;</b> " + item); 
		return Html.fromHtml(TextUtils.join("<br/><br/>", items));
	}
}
