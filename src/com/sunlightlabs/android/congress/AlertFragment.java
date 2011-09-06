package com.sunlightlabs.android.congress;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AlertFragment extends DialogFragment {

	public static final int ABOUT = 1;
	
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
			return aboutDialog(inflater);
		else
			return null;
	}
	
	public Dialog aboutDialog(LayoutInflater inflater) {
		View aboutView = inflater.inflate(R.layout.about, null);

		Spanned about1 = Html.fromHtml(
				"Bill information provided by <a href=\"http://govtrack.us\">GovTrack</a>, " +
				"through the Library of Congress.  Bill summaries written by the Congressional Research Service.<br/><br/>" +
				
				"Votes, committee hearings, and floor updates come from the official " +
				"<a href=\"http://senate.gov/\">Senate</a> and <a href=\"http://clerk.house.gov/\">House</a> websites.<br/><br/>" +
				
				"Legislator and committee information powered by the " + 
				"<a href=\"http://services.sunlightlabs.com/api/\">Sunlight Labs Congress API</a>.<br/><br/>" + 
				
				"News mentions provided by the <a href=\"http://code.google.com/apis/newssearch/v1/\">Google News Search API</a>," +
				" and Twitter search powered by <a href=\"http://www.winterwell.com/software/jtwitter.php\">JTwitter</a>."
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
	
}
