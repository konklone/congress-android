package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.tasks.LoadLegislatorTask;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class BillInfo extends ListActivity implements LoadPhotoTask.LoadsPhoto, LoadBillTask.LoadsBill, LoadLegislatorTask.LoadsLegislator {	
	// fields from the intent 
	private Bill bill;
	private Legislator sponsor;
	private Legislator detailedSponsor;

	// fields fetched remotely
	private String summary;
	
	private LoadBillTask loadBillTask;
	private LoadPhotoTask loadPhotoTask;
	private LoadLegislatorTask loadSponsorTask;
	private View loadingContainer, sponsorView;
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;
	
	private Drawable sponsorPhoto;
	
	private SimpleDateFormat timelineFormat = new SimpleDateFormat("MMM dd, yyyy");
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_bare);
		
		bill = (Bill) getIntent().getExtras().getSerializable("bill");
		sponsor = bill.sponsor;
		
		setupControls();
		
		BillInfoHolder holder = (BillInfoHolder) getLastNonConfigurationInstance();
        if (holder != null) {
        	this.loadBillTask = holder.loadBillTask;
        	this.loadPhotoTask = holder.loadPhotoTask;
        	this.loadSponsorTask = holder.loadSponsorTask;
        	this.summary = holder.summary;
        	this.detailedSponsor = holder.detailedSponsor;
        	this.tracked = holder.tracked;
        }
        
        tracker = Analytics.start(this);
    	if (!tracked) {
			Analytics.page(this, tracker, "/bill/" + bill.id);
			tracked = true;
		}
		
        if (loadSponsorTask != null)
        	loadSponsorTask.onScreenLoad(this);
        
		loadSummary();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Analytics.stop(tracker);
	}
	
	public void setupControls() {
		LayoutInflater inflater = LayoutInflater.from(this);
		MergeAdapter adapter = new MergeAdapter();
		
		View header = inflater.inflate(R.layout.bill_header, null);
		
		TextView titleView = (TextView) header.findViewById(R.id.title);
		String title;
		
		if (bill.short_title != null) {
			title = Utils.truncate(bill.short_title, 400);
			titleView.setTextSize(22);
		} else if (bill.official_title != null) {
			title = bill.official_title;
			titleView.setTextSize(16);
		} else {
			if (bill.abbreviated)
				title = getResources().getString(R.string.bill_no_title_yet);
			else
				title = getResources().getString(R.string.bill_no_title);
			titleView.setTextSize(22);
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
					+ Utils.stateCodeToName(this, sponsor.state);
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
		
		loadingContainer = inflater.inflate(R.layout.header_loading, null);
		((TextView) loadingContainer.findViewById(R.id.header_text)).setText("Summary");
		adapter.addView(loadingContainer);
		
		((TextView) loadingContainer.findViewById(R.id.loading_message)).setText("Loading summary...");
		loadingContainer.findViewById(R.id.loading).setVisibility(View.VISIBLE);
		
		setListAdapter(adapter);
	}
	
	public void displayPhoto() {
		if (sponsorPhoto != null && sponsorView != null)
    		((ImageView) sponsorView.findViewById(R.id.picture)).setImageDrawable(sponsorPhoto);
	}
	
	public void displaySummary() {
		LayoutInflater inflater = LayoutInflater.from(this);
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
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new BillInfoHolder(loadBillTask, loadPhotoTask, loadSponsorTask, summary, detailedSponsor, tracked);
	}
	
	private void loadSponsor() {
		if (loadSponsorTask == null) {
			loadSponsorTask = (LoadLegislatorTask) new LoadLegislatorTask(this).execute(sponsor.getId());
		}
	}
	
	public void loadSummary() {
		if (loadBillTask != null)
			loadBillTask.onScreenLoad(this);
		else {
			if (summary != null)
				displaySummary();
			else
				loadBillTask = (LoadBillTask) new LoadBillTask(this, bill.id).execute("summary");
		}
	}
	
	public void loadPhoto() {
		if (loadPhotoTask != null)
        	loadPhotoTask.onScreenLoad(this);
        else {
        	if (sponsorPhoto != null)
        		displayPhoto();
        	else
				loadPhotoTask = (LoadPhotoTask) new LoadPhotoTask(this, LegislatorImage.PIC_LARGE)
						.execute(sponsor.getId());
        }
	}
	
	public void onLoadBill(Bill bill) {
		this.loadBillTask = null;
		this.summary = bill.summary;
		displaySummary();
		if (sponsor != null)
			loadSponsor();
	}
	
	public void onLoadBill(CongressException exception) {
		this.loadBillTask = null;
		Utils.alert(this, R.string.error_connection);
		finish();
	}
	
	public void onLoadPhoto(Drawable photo, Object tag) {
		sponsorPhoto = photo;
		loadPhotoTask = null;
		displayPhoto();
	}
	
	public void onLoadLegislator(Legislator legislator) {
		loadSponsorTask = null;
		detailedSponsor = legislator;
	}

	public Context getContext() {
		return this;
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
		TextView piece = (TextView) LayoutInflater.from(this).inflate(R.layout.bill_event, null);
		piece.setText(date);
		container.addView(piece);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		String type = (String) v.getTag();
		
		// safety check - don't know why this would happen, but Market error reports imply it can
		if (type == null)
			return;
		
		if (type.equals("sponsor") && sponsor != null)
			startSponsorActivity();
		else if (type.equals("cosponsors")) {
			Intent intent = new Intent(this, LegislatorList.class)
				.putExtra("type", LegislatorList.SEARCH_COSPONSORS)
				.putExtra("bill_id", bill.id);
			startActivity(intent);
		}
    }

	private void startSponsorActivity() {
		if (detailedSponsor != null)
			startActivity(Utils.legislatorTabsIntent().putExtra("legislator", detailedSponsor));
		else
			startActivity(Utils.legislatorLoadIntent(sponsor.getId()));
	}
	
	@Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    super.onCreateOptionsMenu(menu); 
	    getMenuInflater().inflate(R.menu.bill, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.main:
    		startActivity(new Intent(this, MenuMain.class));
    		break;
    	case R.id.shortcut:
			sendBroadcast(Utils.shortcutIntent(this, bill.id, bill.code)
    				.setAction("com.android.launcher.action.INSTALL_SHORTCUT"));
    		break;
    	case R.id.thomas:
    		Analytics.billThomas(this, tracker, bill.id);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.thomasUrl(bill.bill_type, bill.number, bill.session))));
    		break;
    	case R.id.govtrack:
    		Analytics.billGovTrack(this, tracker, bill.id);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.govTrackUrl(bill.bill_type, bill.number, bill.session))));
    		break;
    	case R.id.opencongress:
    		Analytics.billOpenCongress(this, tracker, bill.id);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.openCongressUrl(bill.bill_type, bill.number, bill.session))));
    		break;
    	}
    	return true;
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
	
	static class BillInfoHolder {
		LoadBillTask loadBillTask;
		LoadPhotoTask loadPhotoTask;
		LoadLegislatorTask loadSponsorTask;
		String summary;
		Legislator detailedSponsor;
		boolean tracked;

		public BillInfoHolder(LoadBillTask loadBillTask, LoadPhotoTask loadPhotoTask,
		                      LoadLegislatorTask loadSponsorTask, String summary, 
		                      Legislator detailedSponsor, boolean tracked) {
			this.loadBillTask = loadBillTask;
			this.loadPhotoTask = loadPhotoTask;
			this.loadSponsorTask = loadSponsorTask;
			this.summary = summary;
			this.detailedSponsor = detailedSponsor;
			this.tracked = tracked;
		}
	}
}
