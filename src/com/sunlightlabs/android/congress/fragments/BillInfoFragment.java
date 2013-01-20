package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.models.UpcomingBill;

public class BillInfoFragment extends Fragment implements LoadPhotoTask.LoadsPhoto, LoadBillTask.LoadsBill {	
	// fields from the intent 
	private Bill bill;
	private Legislator sponsor;
	private List<UpcomingBill> latestUpcoming;

	// fields fetched remotely
	private String summary;
	private Drawable sponsorPhoto;
	
	private static SimpleDateFormat timelineFormat = new SimpleDateFormat("MMM dd, yyyy");
	
	public static BillInfoFragment create(Bill bill) {
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
        latestUpcoming = bill.upcoming;
        
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
		// if this was coming in from a search result and has associated highlight data, show it
		if (bill.search != null && bill.search.highlight != null) {
			
			String field = Bill.matchField(bill.search.highlight);
			
			// don't bother showing the short title, or the official title if it's the official title being shown
			if (field != null && !field.equals("popular_title") && !field.equals("keywords") && !field.equals("short_title") && !(field.equals("official_title") && bill.short_title == null)) {
				View searchView = getView().findViewById(R.id.bill_search_data);
				
				String matchText = "\"" + bill.search.query + "\" matched the bill's " + Bill.matchText(field) + ":";
				String highlightText = Utils.truncate(bill.search.highlight.get(field).get(0), 300, false);
				if (field.equals("versions") || field.equals("summary"))
					highlightText = "..." + highlightText + "...";
				
				((TextView) searchView.findViewById(R.id.match_field)).setText(matchText);
				((TextView) searchView.findViewById(R.id.highlight_field)).setText(Html.fromHtml(highlightText));
				
				searchView.setVisibility(View.VISIBLE);
			}
		}
		
		TextView titleView = (TextView) getView().findViewById(R.id.title);
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
			
			String name = sponsor.title + ". " + sponsor.firstName() + " " + sponsor.last_name;
			((TextView) sponsorView.findViewById(R.id.name)).setText(name);
			
			String stateName = Utils.stateCodeToName(getContext(), sponsor.state);
			String description = sponsor.party + " - " + stateName;
			((TextView) sponsorView.findViewById(R.id.description)).setText(description);
			
			sponsorView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(Utils.legislatorLoadIntent(sponsor.id));
				}
			});
			
			// make container of sponsor and cosponsors visible
			getView().findViewById(R.id.bill_all_sponsors).setVisibility(View.VISIBLE);
			
			if (bill.cosponsors_count > 0) {
				View cosponsorView = getView().findViewById(R.id.bill_cosponsors);
				
				((TextView) cosponsorView.findViewById(R.id.bill_cosponsor_number)).setText("+ " + bill.cosponsors_count);
				
				int otherName = (bill.cosponsors_count == 1) ? R.string.bill_cosponsor_other_singular : R.string.bill_cosponsor_other_plural;
				((TextView) cosponsorView.findViewById(R.id.bill_cosponsor_others)).setText(otherName);
				
				cosponsorView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(), LegislatorCosponsors.class)
							.putExtra("billId", bill.id)
							.putExtra("bill", bill);
						startActivity(intent);	
					}
				});
				
				getView().findViewById(R.id.bill_cosponsors_container).setVisibility(View.VISIBLE);
				getView().findViewById(R.id.bill_sponsor_line).setVisibility(View.VISIBLE);
			}
			
			// kick off the photo loading task after the new bill data is all displayed
			loadPhoto();
			
		} else {
			TextView noSponsor = (TextView) getView().findViewById(R.id.bill_no_sponsor);
			noSponsor.setText(R.string.bill_no_sponsor);
			noSponsor.setVisibility(View.VISIBLE);
		}
		
		// prepare the upcoming container if one is necessary
		if (latestUpcoming != null && latestUpcoming.size() > 0) {
			TextView upcomingHeader = (TextView) getView().findViewById(R.id.upcoming_header);
			upcomingHeader.setText(R.string.upcoming_header);
			upcomingHeader.setVisibility(View.VISIBLE);
			
			ViewGroup upcomingContainer = (ViewGroup) getView().findViewById(R.id.upcoming_container);
			for (int i=0; i<latestUpcoming.size(); i++)
				upcomingContainer.addView(upcomingView(latestUpcoming.get(i)));
			upcomingContainer.setVisibility(View.VISIBLE);
		}
		
		((TextView) getView().findViewById(R.id.summary_header)).setText(R.string.bill_summary_header);
		
		View summaryLoading = getView().findViewById(R.id.summary_loading);
		((TextView) summaryLoading.findViewById(R.id.loading_message)).setText("Loading summary...");
		summaryLoading.setVisibility(View.VISIBLE);
	}
	
	public View upcomingView(final UpcomingBill upcoming) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		ViewGroup view = (ViewGroup) inflater.inflate(R.layout.bill_upcoming_item, null);
		
		String text;
		if (upcoming.range == null || upcoming.legislativeDay == null)
			text = "SOMETIME";
		else if (upcoming.range.equals("day"))
			text = Utils.nearbyOrFullDate(upcoming.legislativeDay);
		else if (upcoming.range.equals("week"))
			text = "WEEK OF " + Utils.fullDate(upcoming.legislativeDay);
		else
			text = "SOMETIME";
		
		((TextView) view.findViewById(R.id.date)).setText(text);
		((TextView) view.findViewById(R.id.where)).setText(upcomingSource(upcoming.sourceType, upcoming.chamber));
		
		View moreView = view.findViewById(R.id.more);
		if (upcoming.permalink != null) {
			moreView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Analytics.billUpcomingMore(getActivity(), upcoming.sourceType);
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(upcoming.permalink)));
				}
			});
		} else
			moreView.setVisibility(View.GONE);
		
		return view;
	}
	
	public String upcomingSource(String type, String chamber) {
		if (type.equals("senate_daily"))
			return "On the Senate Floor";
		else if (type.equals("house_daily") || type.equals("house_weekly") || type.equals("house_floor"))
			return "On the House Floor";
		
		// fallbacks, if we add more upcoming source types
		else if (chamber.equals("senate"))
			return "In the Senate";
		else if (chamber.equals("house"))
			return "In the House";
		
		else // should never happen
			return "In Congress";
	}
	
	public void displayPhoto() {
		View sponsorView = getView().findViewById(R.id.bill_sponsor);
    	((ImageView) sponsorView.findViewById(R.id.picture)).setImageDrawable(sponsorPhoto);
	}
	
	public void displaySummary() {
		getView().findViewById(R.id.summary_loading).setVisibility(View.GONE);
		 
		if (summary != null && summary.length() > 0) {
			String formatted = Bill.formatSummary(summary, bill.short_title);
			TextView summaryView = (TextView) getView().findViewById(R.id.bill_summary);
			summaryView.setText(formatted);
			summaryView.setVisibility(View.VISIBLE);
		} else {
			TextView noSummary = (TextView) getView().findViewById(R.id.bill_no_summary);
			if (bill.urls != null && bill.urls.containsKey("html")) {
				noSummary.setText(Html.fromHtml("No summary available.<br/><br/><a href=\""
					+ bill.urls.get("html")
					+ "\">Read the full text.</a>"));
			} else if (bill.urls != null && bill.urls.containsKey("pdf")) {
				noSummary.setText(Html.fromHtml("No summary available.<br/><br/><a href=\""
					+ bill.urls.get("pdf")
					+ "\">Read the full text (PDF).</a>"));
			} else {
				noSummary.setText(Html.fromHtml("No summary available.<br/><br/><a href=\""
						+ Bill.thomasUrl(bill.bill_type, bill.number, bill.congress)
						+ "\">Read the text of this bill on THOMAS.</a>"));
			}
        	noSummary.setMovementMethod(LinkMovementMethod.getInstance());
        	noSummary.setVisibility(View.VISIBLE);
		}
	}
	
	public void loadSummary() {
		new LoadBillTask(this, bill.id).execute("summary");
	}
	
	public void loadPhoto() {
		new LoadPhotoTask(this, LegislatorImage.PIC_LARGE).execute(sponsor.id);
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
		ViewGroup inner = (ViewGroup) getView().findViewById(R.id.header_inner);
		
		if (bill.introduced_on != null)
			addTimelinePiece(inner, "Introduced on", bill.introduced_on.getTime());
		
		String house_passage_result = bill.house_passage_result;
		long house_passage_result_at = bill.house_passage_result_at == null ? 0 : bill.house_passage_result_at.getTime();
		if (house_passage_result != null && house_passage_result_at > 0) {
			if (house_passage_result.equals("pass"))
				addTimelinePiece(inner, "Passed the House on", house_passage_result_at);
			else if (house_passage_result.equals("fail"))
				addTimelinePiece(inner, "Failed the House on", house_passage_result_at);
		}
		
		String senate_cloture_result = bill.senate_cloture_result;
		long senate_cloture_result_at = bill.senate_cloture_result_at == null ? 0 : bill.senate_cloture_result_at.getTime();
		if (senate_cloture_result != null && senate_cloture_result_at > 0) {
			if (senate_cloture_result.equals("pass"))
				addTimelinePiece(inner, "Passed cloture in the Senate on", senate_cloture_result_at);
			else if (senate_cloture_result.equals("fail"))
				addTimelinePiece(inner, "Failed cloture in the Senate on", senate_cloture_result_at);
		}
		
		String senate_passage_result = bill.senate_passage_result;
		long senate_passage_result_at = bill.senate_passage_result_at == null ? 0 : bill.senate_passage_result_at.getTime();
		if (senate_passage_result != null && senate_passage_result_at > 0) {
			if (senate_passage_result.equals("pass"))
				addTimelinePiece(inner, "Passed the Senate on", senate_passage_result_at);
			else if (senate_passage_result.equals("fail"))
				addTimelinePiece(inner, "Failed the Senate on", senate_passage_result_at);
		}
		
		long vetoed_at = bill.vetoed_at == null ? 0 : bill.vetoed_at.getTime();
		if (bill.vetoed && vetoed_at > 0)
			addTimelinePiece(inner, "Vetoed on", vetoed_at);
		
		String house_override_result = bill.house_override_result;
		long house_override_result_at = bill.house_override_result_at == null ? 0 : bill.house_override_result_at.getTime();
		if (house_override_result != null && house_override_result_at > 0) {
			if (house_override_result.equals("pass"))
				addTimelinePiece(inner, "Override passed in the House on", house_override_result_at);
			else if (house_override_result.equals("fail"))
				addTimelinePiece(inner, "Override failed in the House on", house_override_result_at);
		}
		
		String senate_override_result = bill.senate_override_result;
		long senate_override_result_at = bill.senate_override_result_at == null ? 0 : bill.senate_override_result_at.getTime();
		if (senate_override_result != null && senate_override_result_at > 0) {
			if (senate_override_result.equals("pass"))
				addTimelinePiece(inner, "Override passed in the Senate on", senate_override_result_at);
			else if (senate_override_result.equals("fail"))
				addTimelinePiece(inner, "Override failed in the Senate on", senate_override_result_at);
		}
		
		long awaiting_signature_since = bill.awaiting_signature_since == null ? 0 : bill.awaiting_signature_since.getTime();
		if (bill.awaiting_signature && awaiting_signature_since > 0)
			addTimelinePiece(inner, "Awaiting signature since", awaiting_signature_since);
		
		long enacted_at = bill.enacted_at == null ? 0 : bill.enacted_at.getTime();
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