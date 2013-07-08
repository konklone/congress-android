package com.sunlightlabs.android.congress.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;

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
			.setPositiveButton(R.string.first_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			})
			.create();
	}
	
	public Dialog about(LayoutInflater inflater) {
		View aboutView = inflater.inflate(R.layout.about, null);

		Spanned about1 = Html.fromHtml(
				"<b>Have a friend with an iPhone?</b> Tell them to <a href=\"http://congress.sunlightfoundation.com/\">check out our iOS app</a>.<br/><br/>" +
				"Bill information provided by the <a href=\"http://congress.gov\">Library of Congress</a>.  Bill summaries written by the Congressional Research Service.<br/><br/>" +
				
				"Votes, committee hearings, and floor updates come from official " +
				"<a href=\"http://senate.gov/\">Senate</a> and <a href=\"http://clerk.house.gov/\">House</a> websites.<br/><br/>" +
				
				"People and committee information powered by the " + 
				"<a href=\"https://github.com/unitedstates\">github.com/unitedstates</a> project.<br/><br/>" + 
				
				"News mentions provided by the <a href=\"http://code.google.com/apis/newssearch/v1/\">Google News Search API</a>."
		);
		TextView aboutView1 = (TextView) aboutView.findViewById(R.id.about_1);
		aboutView1.setText(about1);
		aboutView1.setMovementMethod(LinkMovementMethod.getInstance());

		Spanned about2 = Html.fromHtml(
				"This app is made by the <a href=\"http://sunlightfoundation.com\">Sunlight Foundation</a>, " +
				"a non-partisan non-profit dedicated to increasing government transparency through the power of technology."
		);
		TextView aboutView2 = (TextView) aboutView.findViewById(R.id.about_2);
		aboutView2.setText(about2);
		aboutView2.setMovementMethod(LinkMovementMethod.getInstance());
		
		return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.icon)
			.setView(aboutView)
			.setPositiveButton(R.string.about_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
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
		TextView titleText = (TextView) title.findViewById(R.id.title);
		titleText.setText(getResources().getString(R.string.changelog_title_prefix) + " " + getResources().getString(R.string.app_version));
		
		return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.icon)
			.setCustomTitle(title)
			.setView(changelogView)
			.setPositiveButton(R.string.changelog_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			})
			.create();
	}
	
	private Spanned getChangelogHtml(int stringArrayId) {
		String[] array = getActivity().getResources().getStringArray(stringArrayId);
		List<String> items = new ArrayList<String>();
		for (String item : array)
			items.add("<b>&#183;</b> " + item); 
		return Html.fromHtml(TextUtils.join("<br/><br/>", items));
	}
	
}
