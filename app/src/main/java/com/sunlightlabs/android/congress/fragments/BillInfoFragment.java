package com.sunlightlabs.android.congress.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.LegislatorCosponsors;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BillInfoFragment extends Fragment implements LoadPhotoTask.LoadsPhoto, LoadBillTask.LoadsBill {
	// fields from the intent 
	private Bill bill;
	private Legislator sponsor;

	// fields fetched remotely
	private String summary;
	private Drawable sponsorPhoto;

	private static SimpleDateFormat timelineFormat = new SimpleDateFormat("MMM dd, yyyy");

	public static android.support.v4.app.Fragment create(Bill bill) {
		BillInfoFragment frag = new BillInfoFragment();
		Bundle args = new Bundle();
		args.putSerializable("bill", bill);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}

	public BillInfoFragment() {}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentUtils.setupAPI(this);

        bill = (Bill) getArguments().getSerializable("bill");
        sponsor = bill.sponsor;

        loadSummary();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.bill, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setupControls();

		if (summary != null)
			displaySummary();

		if (sponsorPhoto != null)
    		displayPhoto();
	}

	public void setupControls() {

		TextView titleView = getView().findViewById(R.id.title);
		String title;

		if (bill.short_title != null) {
			title = Utils.truncate(bill.short_title, 400);
			titleView.setTextSize(18);
		} else if (bill.official_title != null) {
			title = bill.official_title;
			titleView.setTextSize(16);
		} else {
			title = getResources().getString(R.string.bill_no_title);
			titleView.setTextSize(18);
		}
		titleView.setText(title);

		addBillTimeline();

		if (sponsor != null) {
			View sponsorView = getView().findViewById(R.id.bill_sponsor);

			((TextView) sponsorView.findViewById(R.id.name)).setText(sponsor.titledName());

			String stateName = Utils.stateCodeToName(getContext(), sponsor.state);
			String description = sponsor.party + " - " + stateName;
			((TextView) sponsorView.findViewById(R.id.description)).setText(description);

			sponsorView.setOnClickListener(v -> startActivity(Utils.legislatorIntent(sponsor.bioguide_id)));

			// make container of sponsor and cosponsors visible
			getView().findViewById(R.id.bill_all_sponsors).setVisibility(View.VISIBLE);

			if (bill.cosponsors_count > 0) {
				View cosponsorView = getView().findViewById(R.id.bill_cosponsors);

				((TextView) cosponsorView.findViewById(R.id.bill_cosponsor_number)).setText(R.string.plus + bill.cosponsors_count);

				int otherName = (bill.cosponsors_count == 1) ? R.string.bill_cosponsor_other_singular : R.string.bill_cosponsor_other_plural;
				((TextView) cosponsorView.findViewById(R.id.bill_cosponsor_others)).setText(otherName);

				cosponsorView.setOnClickListener(v -> {
					Intent intent = new Intent(getActivity(), LegislatorCosponsors.class)
							.putExtra("billId", bill.id)
							.putExtra("bill", bill);
					startActivity(intent);
				});

				getView().findViewById(R.id.bill_cosponsors_container).setVisibility(View.VISIBLE);
				getView().findViewById(R.id.bill_sponsor_line).setVisibility(View.VISIBLE);
			}

			// kick off the photo loading task after the new bill data is all displayed
			loadPhoto();

		} else {
			TextView noSponsor = getView().findViewById(R.id.bill_no_sponsor);
			noSponsor.setText(R.string.bill_no_sponsor);
			noSponsor.setVisibility(View.VISIBLE);
		}

		((TextView) getView().findViewById(R.id.summary_header)).setText(R.string.bill_summary_header);

		View summaryLoading = getView().findViewById(R.id.summary_loading);
		((TextView) summaryLoading.findViewById(R.id.loading_message)).setText(R.string.loading_summary);
		summaryLoading.setVisibility(View.VISIBLE);
	}

	public void displayPhoto() {
		View sponsorView = getView().findViewById(R.id.bill_sponsor);
    	((ImageView) sponsorView.findViewById(R.id.picture)).setImageDrawable(sponsorPhoto);
	}

	public void displaySummary() {
		getView().findViewById(R.id.summary_loading).setVisibility(View.GONE);

		if (summary != null && summary.length() > 0) {
			String formatted = Bill.formatSummary(summary, bill.short_title);
			TextView summaryView = getView().findViewById(R.id.bill_summary);
			summaryView.setText(formatted);
			summaryView.setVisibility(View.VISIBLE);
		} else {
			TextView noSummary = getView().findViewById(R.id.bill_no_summary);
			noSummary.setText(Html.fromHtml("No summary available.<br/><br/><a href=\""
				+ bill.bestFullTextUrl()
				+ "\">Read the official description.</a>"));
			noSummary.setMovementMethod(LinkMovementMethod.getInstance());
        	noSummary.setVisibility(View.VISIBLE);
		}
	}

	public void loadSummary() {
		new LoadBillTask(this, bill.id).execute();
	}

	public void loadPhoto() {
		new LoadPhotoTask(this, LegislatorImage.PIC_LARGE).execute(sponsor.bioguide_id);
	}

	public void onLoadBill(Bill bill) {
		if (bill.summary != null)
			this.summary = bill.summary;
		else
			this.summary = "";

		if (isAdded())
			displaySummary();
	}

	public void onLoadBill(CongressException exception) {
		if (isAdded())
			Utils.alert(getActivity(), R.string.error_connection);
	}

	public void onLoadPhoto(Drawable photo, Object tag) {
		sponsorPhoto = photo;
		if (isAdded())
			displayPhoto();
	}

	// Take the layout view given, and append all applicable bill_event TextViews
	// describing the basic timeline of the bill
	public void addBillTimeline() {
		ViewGroup inner = getView().findViewById(R.id.header_inner);

		if (bill.introduced_on != null)
			addTimelinePiece(inner, "Introduced on", bill.introduced_on.getTime());

		long house_passage_result_at = bill.house_passage_result_on == null ? 0 : bill.house_passage_result_on.getTime();
		if (house_passage_result_at > 0)
			addTimelinePiece(inner, "Passed the House on", house_passage_result_at);

		long senate_passage_result_at = bill.senate_passage_result_on == null ? 0 : bill.senate_passage_result_on.getTime();
		if (senate_passage_result_at > 0)
			addTimelinePiece(inner, "Passed the Senate on", senate_passage_result_at);

		long vetoed_at = bill.vetoed_on == null ? 0 : bill.vetoed_on.getTime();
		if (bill.vetoed && vetoed_at > 0)
			addTimelinePiece(inner, "Vetoed on", vetoed_at);

		long enacted_at = bill.enacted_on == null ? 0 : bill.enacted_on.getTime();
		if (bill.enacted && enacted_at > 0)
			addTimelinePiece(inner, "Enacted on", enacted_at);
	}

	public void addTimelinePiece(ViewGroup container, String prefix, long timestamp) {
		String date = prefix + " " + timelineFormat.format(new Date(timestamp));
		TextView piece = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.bill_event, null);
		piece.setText(date);
		container.addView(piece);
	}

	public int sizeOfTitle(String title) {
		int length = title.length();
		if (length <= 100)
			return 18;
		else if (length <= 200)
			return 16;
		else if (length <= 300)
			return 14;
		else if (length <= 400)
			return 12;
		else // should be truncated above this anyhow
			return 12;
	}

	public Context getContext() {
		return getActivity();
	}
}