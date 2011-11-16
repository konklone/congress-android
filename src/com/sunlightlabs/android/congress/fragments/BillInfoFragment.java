package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.LegislatorList;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.tasks.LoadLegislatorTask;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.models.UpcomingBill;

public class BillInfoFragment extends ListFragment implements LoadPhotoTask.LoadsPhoto, LoadBillTask.LoadsBill, LoadLegislatorTask.LoadsLegislator {	
	// fields from the intent 
	private Bill bill;
	private Legislator sponsor;
	private Legislator detailedSponsor;

	// fields fetched remotely
	private String summary;
	private List<UpcomingBill> latestUpcoming;
	
	private View loadingContainer, sponsorView;
	
	private Drawable sponsorPhoto;
	
	private SimpleDateFormat timelineFormat = new SimpleDateFormat("MMM dd, yyyy");
	
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
        
        FragmentUtils.setupRTC(this);
        
        bill = (Bill) getArguments().getSerializable("bill");
        sponsor = bill.sponsor;
        
        // filter out old upcoming activity (don't depend on server to flush it out)
        latestUpcoming = bill.upcomingSince(GregorianCalendar.getInstance().getTime());
        
        loadSummary();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_bare_no_divider, container, false);
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
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		MergeAdapter adapter = new MergeAdapter();
		
		// if this was coming in from a search result and has associated highlight data, show it
		if (bill.search != null && bill.search.highlight != null) {
			
			String field = Bill.matchField(bill.search.highlight);
			
			// don't bother showing the short title, or the official title if it's the official title being shown
			if (!field.equals("short_title") && !(field.equals("official_title") && bill.short_title == null)) {
				final View searchView = inflater.inflate(R.layout.bill_search_data, null);
				
				String matchText = "\"" + bill.search.query + "\" matched the bill's " + Bill.matchText(field) + ":";
				String highlightText = Utils.truncate(bill.search.highlight.get(field).get(0), 300, false);
				if (field.equals("versions") || field.equals("summary"))
					highlightText = "..." + highlightText + "...";
				
				((TextView) searchView.findViewById(R.id.match_field)).setText(matchText);
				((TextView) searchView.findViewById(R.id.highlight_field)).setText(Html.fromHtml(highlightText));
				
				adapter.addView(searchView);
			}
		}
		
		View header = inflater.inflate(R.layout.bill_header, null);
		
		TextView titleView = (TextView) header.findViewById(R.id.title);
		String title;
		
		if (bill.short_title != null) {
			title = Utils.truncate(bill.short_title, 400);
			titleView.setTextSize(18);
		} else if (bill.official_title != null) {
			title = bill.official_title;
			titleView.setTextSize(16);
		} else {
			if (bill.abbreviated)
				title = getResources().getString(R.string.bill_no_title_yet);
			else
				title = getResources().getString(R.string.bill_no_title);
			titleView.setTextSize(18);
		}
		titleView.setText(title);
		
		if (!bill.abbreviated)
			addBillTimeline(header);
		
		adapter.addView(header);
		
		List<View> listViews = new ArrayList<View>();
		if (sponsor != null) {
			sponsorView = inflater.inflate(R.layout.bill_sponsor, null);
			
			String name = sponsor.title + ". " + sponsor.firstName() + " " + sponsor.last_name;
			((TextView) sponsorView.findViewById(R.id.name)).setText(name);
			
			String description = Legislator.partyName(sponsor.party) + " from "
					+ Utils.stateCodeToName(getActivity(), sponsor.state);
			((TextView) sponsorView.findViewById(R.id.description)).setText(description);
			
			sponsorView.setTag("sponsor");
			listViews.add(sponsorView);
			
			// kick off the photo loading task after the new bill data is all displayed
			loadPhoto();
		} else {
			View noSponsor = inflater.inflate(R.layout.bill_no_sponsor, null);
			((TextView) noSponsor.findViewById(R.id.text))
				.setText(bill.abbreviated ? R.string.bill_no_sponsor_yet : R.string.bill_no_sponsor);
			adapter.addView(noSponsor);
		}
		
		if (bill.cosponsors_count > 0) {
			View cosponsorView = inflater.inflate(R.layout.bill_cosponsors, null);
			String cosponsorText = bill.cosponsors_count + (bill.cosponsors_count == 1 ? " Cosponsor" : " Cosponsors");
			((ImageView) cosponsorView.findViewById(R.id.icon)).setImageResource(R.drawable.people);
			((TextView) cosponsorView.findViewById(R.id.text)).setText(cosponsorText);
			cosponsorView.setTag("cosponsors");
			listViews.add(cosponsorView);
		}
		
		if (!listViews.isEmpty())
			adapter.addAdapter(new ViewArrayAdapter(this, listViews));
		
		// prepare the upcoming container if one is necessary
		if (latestUpcoming != null && latestUpcoming.size() > 0) {
			TextView upcomingHeader = (TextView) inflater.inflate(R.layout.header, null);
			upcomingHeader.setText(R.string.upcoming_header);
			
			ViewGroup upcomingContainer = (ViewGroup) inflater.inflate(R.layout.bill_upcoming, null);
			for (int i=0; i<latestUpcoming.size(); i++)
				upcomingContainer.addView(upcomingView(latestUpcoming.get(i)));
			
			adapter.addView(upcomingHeader);
			adapter.addView(upcomingContainer);
		}
		
		loadingContainer = inflater.inflate(R.layout.header_loading, null);
		((TextView) loadingContainer.findViewById(R.id.header_text)).setText("Summary");
		adapter.addView(loadingContainer);
		
		((TextView) loadingContainer.findViewById(R.id.loading_message)).setText("Loading summary...");
		loadingContainer.findViewById(R.id.loading).setVisibility(View.VISIBLE);
		
		setListAdapter(adapter);
	}
	
	public View upcomingView(final UpcomingBill upcoming) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		ViewGroup view = (ViewGroup) inflater.inflate(R.layout.bill_upcoming_item, null);
		
		((TextView) view.findViewById(R.id.date)).setText(Utils.upcomingDate(upcoming.legislativeDay));
		((TextView) view.findViewById(R.id.where)).setText(upcomingSource(upcoming.sourceType, upcoming.chamber));
		
		View moreView = view.findViewById(R.id.more);
		if (upcoming.permalink != null) {
			moreView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Analytics.billUpcoming(getActivity(), upcoming.sourceType);
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
		else if (type.equals("house_daily"))
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
		if (sponsorView != null)
    		((ImageView) sponsorView.findViewById(R.id.picture)).setImageDrawable(sponsorPhoto);
	}
	
	public void displaySummary() {
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		MergeAdapter adapter = (MergeAdapter) getListAdapter();
		
		loadingContainer.findViewById(R.id.loading).setVisibility(View.GONE);
		
		View summaryView; 
		if (summary != null && summary.length() > 0) {
			summaryView = inflater.inflate(R.layout.bill_summary, null);
			String formatted = Bill.formatSummary(summary, bill.short_title);
			((TextView) summaryView.findViewById(R.id.summary)).setText(formatted);
		} else {
			summaryView = inflater.inflate(R.layout.bill_no_summary, null);
			TextView noSummary = (TextView) summaryView.findViewById(R.id.no_summary);
			noSummary.setText(Html.fromHtml("No summary available.<br/><br/><a href=\""
					+ Bill.thomasUrl(bill.bill_type, bill.number, bill.session)
					+ "\">Read the text of this bill on THOMAS.</a>"));
        	noSummary.setMovementMethod(LinkMovementMethod.getInstance());
		}
		adapter.addView(summaryView);
		setListAdapter(adapter);
	}
	
	private void loadSponsor() {
		new LoadLegislatorTask(this).execute(sponsor.getId());
	}
	
	public void loadSummary() {
		new LoadBillTask(this, bill.id).execute("summary");
	}
	
	public void loadPhoto() {
		new LoadPhotoTask(this, LegislatorImage.PIC_LARGE).execute(sponsor.getId());
	}
	
	public void onLoadBill(Bill bill) {
		this.summary = bill.summary;
		
		if (isAdded())
			displaySummary();
		
		if (sponsor != null)
			loadSponsor();
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
	
	public void onLoadLegislator(Legislator legislator) {
		detailedSponsor = legislator;
	}
	
	
	// Take the layout view given, and append all applicable bill_event TextViews
	// describing the basic timeline of the bill
	public void addBillTimeline(View header) {
		ViewGroup inner = (ViewGroup) header.findViewById(R.id.header_inner);
		
		if (bill.introduced_at != null)
			addTimelinePiece(inner, "Introduced on", bill.introduced_at.getTime());
		
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
	
	@Override
    public void onListItemClick(ListView l, View v, int position, long id) {
		String type = (String) v.getTag();
		
		// safety check - don't know why this would happen, but Market error reports imply it can
		if (type == null)
			return;
		
		if (type.equals("sponsor") && sponsor != null)
			startSponsorActivity();
		else if (type.equals("cosponsors")) {
			Intent intent = new Intent(getActivity(), LegislatorList.class)
				.putExtra("type", LegislatorList.SEARCH_COSPONSORS)
				.putExtra("bill_id", bill.id);
			startActivity(intent);
		}
    }

	private void startSponsorActivity() {
		if (detailedSponsor != null)
			startActivity(Utils.legislatorPagerIntent().putExtra("legislator", detailedSponsor));
		else
			startActivity(Utils.legislatorLoadIntent(sponsor.getId()));
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