package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.LoadBillTask;
import com.sunlightlabs.android.congress.utils.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class BillInfo extends ListActivity implements LoadPhotoTask.LoadsPhoto, LoadBillTask.LoadsBill {	
	private Bill bill;
	private Legislator sponsor;

	// fields fetched remotely
	private String summary;
	
	private LoadBillTask loadBillTask;
	private LoadPhotoTask loadPhotoTask;
	private View loadingContainer, sponsorView;
	
	private Drawable sponsorPhoto;
	
	private SimpleDateFormat timelineFormat = new SimpleDateFormat("MMM dd, yyyy");
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		bill = (Bill) getIntent().getExtras().getSerializable("bill");
		sponsor = bill.sponsor;
		
		setupControls();
		
		BillInfoHolder holder = (BillInfoHolder) getLastNonConfigurationInstance();
        if (holder != null) {
        	loadBillTask = holder.loadBillTask;
        	loadPhotoTask = holder.loadPhotoTask;
        	summary = holder.summary;
        }
		
		loadSummary();
	}
	
	public void setupControls() {
		LayoutInflater inflater = LayoutInflater.from(this);
		MergeAdapter adapter = new MergeAdapter();
		
		View header = inflater.inflate(R.layout.bill_header, null);
		
		TextView titleView = (TextView) header.findViewById(R.id.title);
		String title;
		String short_title = bill.short_title;
		if (short_title != null) {
			title = Utils.truncate(short_title, 400);
			titleView.setTextSize(22);
		} else {
			title = bill.official_title;
			titleView.setTextSize(16);
		}
		titleView.setText(title);
		
		addBillTimeline(header);
		
		adapter.addView(header);
		
		if (sponsor != null) {
			sponsorView = inflater.inflate(R.layout.bill_sponsor, null);
			
			String name = sponsor.title + ". " + sponsor.firstName() + " " + sponsor.last_name;
			((TextView) sponsorView.findViewById(R.id.name)).setText(name);
			
			String description = Legislator.partyName(sponsor.party) + " from "
					+ Utils.stateCodeToName(this, sponsor.state);
			((TextView) sponsorView.findViewById(R.id.description)).setText(description);
			
			sponsorView.setTag("sponsor");
			ArrayList<View> sponsorViews = new ArrayList<View>(1);
			sponsorViews.add(sponsorView);
			adapter.addAdapter(new ViewArrayAdapter(this, sponsorViews));
			
			// kick off the photo loading task after the new bill data is all displayed
			loadPhoto();
		} else
			adapter.addView(inflater.inflate(R.layout.bill_no_sponsor, null));
		
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
					+ Bill.thomasUrl(bill.type, bill.number, bill.session)
					+ "\">Read the text of this bill on THOMAS.</a>"));
        	noSummary.setMovementMethod(LinkMovementMethod.getInstance());
		}
		adapter.addView(summaryView);
		setListAdapter(adapter);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new BillInfoHolder(loadBillTask, loadPhotoTask, summary);
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
	
	public Context getContext() {
		return this;
	}
	
	
	// Take the layout view given, and append all applicable bill_event TextViews
	// describing the basic timeline of the bill
	public void addBillTimeline(View header) {
		ViewGroup inner = (ViewGroup) header.findViewById(R.id.header_inner);
		
		addTimelinePiece(inner, "Introduced on", bill.introduced_at.getTime());
		
		String house_result = bill.house_result;
		long house_result_at = bill.house_result_at == null ? 0 : bill.house_result_at.getTime();
		if (house_result != null && house_result_at > 0) {
			if (house_result.equals("pass"))
				addTimelinePiece(inner, "Passed the House on", house_result_at);
			else if (house_result.equals("fail"))
				addTimelinePiece(inner, "Failed the House on", house_result_at);
		}
		
		String senate_result = bill.senate_result;
		long senate_result_at = bill.senate_result_at == null ? 0 : bill.senate_result_at.getTime();
		if (senate_result != null && senate_result_at > 0) {
			if (senate_result.equals("pass"))
				addTimelinePiece(inner, "Passed the Senate on", senate_result_at);
			else if (senate_result.equals("fail"))
				addTimelinePiece(inner, "Failed the Senate on", senate_result_at);
		}
		
		long vetoed_at = bill.vetoed_at == null ? 0 : bill.vetoed_at.getTime();
		if (bill.vetoed && vetoed_at > 0)
			addTimelinePiece(inner, "Vetoed on", vetoed_at);
		
		String override_house_result = bill.override_house_result;
		long override_house_result_at = bill.override_house_result_at == null ? 0 : bill.override_house_result_at.getTime();
		if (override_house_result != null && override_house_result_at > 0) {
			if (override_house_result.equals("pass"))
				addTimelinePiece(inner, "Override passed in the House on", override_house_result_at);
			else if (override_house_result.equals("fail"))
				addTimelinePiece(inner, "Override failed in the House on", override_house_result_at);
		}
		
		String override_senate_result = bill.override_senate_result;
		long override_senate_result_at = bill.override_house_result_at == null ? 0 : bill.override_senate_result_at.getTime();
		if (override_senate_result != null && override_senate_result_at > 0) {
			if (override_senate_result.equals("pass"))
				addTimelinePiece(inner, "Override passed in the Senate on", override_senate_result_at);
			else if (override_senate_result.equals("fail"))
				addTimelinePiece(inner, "Override failed in the Senate on", override_senate_result_at);
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
		if (type.equals("sponsor") && sponsor != null)
			startActivity(Utils.legislatorIntent(sponsor.getId()));
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
    		startActivity(new Intent(this, MainMenu.class));
    		break;
    	case R.id.shortcut:
			sendBroadcast(Utils.shortcutIntent(this, bill.id, bill.code)
    				.setAction("com.android.launcher.action.INSTALL_SHORTCUT"));
    		break;
    	case R.id.thomas:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.thomasUrl(bill.type,
					bill.number, bill.session))));
    		break;
    	case R.id.govtrack:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.govTrackUrl(bill.type,
					bill.number, bill.session))));
    		break;
    	case R.id.opencongress:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.openCongressUrl(bill.type,
					bill.number, bill.session))));
    		break;
    	case R.id.share:
    		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText());
    		startActivity(Intent.createChooser(intent, "Share bill"));
    		break;
    	}
    	return true;
    }
	
	public String shareText() {
		String url = Bill.thomasUrl(bill.type, bill.number, bill.session);
		String short_title = bill.short_title;
		if (short_title != null && !short_title.equals(""))
			return "Check out the " + short_title + " on THOMAS: " + url;
		else
			return "Check out the bill " + Bill.formatCode(bill.code) + " on THOMAS: " + url;
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
		String summary;
		
		public BillInfoHolder(LoadBillTask loadBillTask, LoadPhotoTask loadPhotoTask, String summary) {
			this.loadBillTask = loadBillTask;
			this.loadPhotoTask = loadPhotoTask;
			this.summary = summary;
		}
	}
}