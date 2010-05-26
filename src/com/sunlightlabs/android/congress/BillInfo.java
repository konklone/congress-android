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
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.LoadBillTask;
import com.sunlightlabs.android.congress.utils.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.LoadsBill;
import com.sunlightlabs.android.congress.utils.LoadsPhoto;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Legislator;

public class BillInfo extends ListActivity implements LoadsPhoto, LoadsBill {	
	// fields from the intent 
	private String id, type, code, short_title, official_title;
	private int number, session;
	private boolean passed, vetoed, awaiting_signature, enacted;
	private String house_result, senate_result, override_house_result, override_senate_result;
	private long introduced_at, house_result_at, senate_result_at, passed_at;
	private long vetoed_at, override_house_result_at, override_senate_result_at;
	private long awaiting_signature_since, enacted_at;
	private String sponsor_id, sponsor_party, sponsor_state, sponsor_title;
	private String sponsor_first_name, sponsor_last_name, sponsor_nickname;
	
	// fields fetched remotely
	private String summary;
	
	private LoadBillTask loadBillTask;
	private LoadPhotoTask loadPhotoTask;
	private LinearLayout loadingContainer, sponsorView;
	
	private Drawable sponsorPhoto;
	
	private SimpleDateFormat timelineFormat = new SimpleDateFormat("MMM dd, yyyy");
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		type = extras.getString("type");
		number = extras.getInt("number");
		session = extras.getInt("session");
		code = extras.getString("code");
		short_title = extras.getString("short_title");
		official_title = extras.getString("official_title");
		
		introduced_at = extras.getLong("introduced_at", 0);
		house_result = extras.getString("house_result");
		house_result_at = extras.getLong("house_result_at", 0);
		senate_result = extras.getString("senate_result");
		senate_result_at = extras.getLong("senate_result_at", 0);
		passed = extras.getBoolean("passed", false);
		passed_at = extras.getLong("passed_at", 0);
		vetoed = extras.getBoolean("vetoed", false);
		vetoed_at = extras.getLong("vetoed_at", 0);
		override_house_result = extras.getString("override_house_result");
		override_house_result_at = extras.getLong("override_house_result_at", 0);
		override_senate_result = extras.getString("override_senate_result");
		override_senate_result_at = extras.getLong("override_senate_result_at", 0);
		awaiting_signature = extras.getBoolean("awaiting_signature", false);
		awaiting_signature_since = extras.getLong("awaiting_signature_since", 0);
		enacted = extras.getBoolean("enacted", false);
		enacted_at = extras.getLong("enacted_at", 0);
		
		sponsor_id = extras.getString("sponsor_id");
		sponsor_title = extras.getString("sponsor_title");
		sponsor_state = extras.getString("sponsor_state");
		sponsor_party = extras.getString("sponsor_party");
		sponsor_first_name = extras.getString("sponsor_first_name");
		sponsor_nickname = extras.getString("sponsor_nickname");
		sponsor_last_name = extras.getString("sponsor_last_name");
		
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
		
		LinearLayout header = (LinearLayout) inflater.inflate(R.layout.bill_header, null);
		
		TextView titleView = (TextView) header.findViewById(R.id.title);
		String title;
		if (short_title != null) {
			title = Utils.truncate(short_title, 400);
			titleView.setTextSize(22);
		} else {
			title = official_title;
			titleView.setTextSize(16);
		}
		titleView.setText(title);
		
		addBillTimeline(header);
		
		adapter.addView(header);
		
		if (sponsor_id != null) {
			sponsorView = (LinearLayout) inflater.inflate(R.layout.bill_sponsor, null);
			String firstname;
			if (sponsor_nickname != null && !sponsor_nickname.equals(""))
				firstname = sponsor_nickname;
			else
				firstname = sponsor_first_name;
			
			String name = sponsor_title + ". " + firstname + " " + sponsor_last_name;
			((TextView) sponsorView.findViewById(R.id.name)).setText(name);
			
			String description = Legislator.partyName(sponsor_party) + " from " + Utils.stateCodeToName(this, sponsor_state);
			((TextView) sponsorView.findViewById(R.id.description)).setText(description);
			
			sponsorView.setTag("sponsor");
			ArrayList<View> sponsorViews = new ArrayList<View>(1);
			sponsorViews.add(sponsorView);
			adapter.addAdapter(new ViewArrayAdapter(this, sponsorViews));
			
			// kick off the photo loading task after the new bill data is all displayed
			loadPhoto();
		} else
			adapter.addView(inflater.inflate(R.layout.bill_no_sponsor, null));
		
		loadingContainer = (LinearLayout) inflater.inflate(R.layout.header_loading, null);
		((TextView) loadingContainer.findViewById(R.id.header_text)).setText("Summary");
		adapter.addView(loadingContainer);
		
		((TextView) loadingContainer.findViewById(R.id.loading_message)).setText("Loading summary...");
		loadingContainer.findViewById(R.id.loading).setVisibility(View.VISIBLE);
		setProgressBarIndeterminateVisibility(true);
		
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
		setProgressBarIndeterminateVisibility(false);
		
		LinearLayout summaryView; 
		if (summary != null && summary.length() > 0) {
			summaryView = (LinearLayout) inflater.inflate(R.layout.bill_summary, null);
			String formatted = Bill.formatSummary(summary, short_title);
			((TextView) summaryView.findViewById(R.id.summary)).setText(formatted);
		} else {
			summaryView = (LinearLayout) inflater.inflate(R.layout.bill_no_summary, null);
			TextView noSummary = (TextView) summaryView.findViewById(R.id.no_summary);
			noSummary.setText(Html.fromHtml("No summary available.<br/><br/><a href=\"" + Bill.thomasUrl(type, number, session) + "\">Read the text of this bill on THOMAS.</a>"));
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
				loadBillTask = (LoadBillTask) new LoadBillTask(this, id).execute("summary");
		}
	}
	
	public void loadPhoto() {
		if (loadPhotoTask != null)
        	loadPhotoTask.onScreenLoad(this);
        else {
        	if (sponsorPhoto != null)
        		displayPhoto();
        	else
        		loadPhotoTask = (LoadPhotoTask) new LoadPhotoTask(this, LegislatorImage.PIC_LARGE).execute(sponsor_id);
        }
	}
	
	public void onLoadBill(Bill bill) {
		this.loadBillTask = null;
		this.summary = bill.summary;
		displaySummary();
	}
	
	public void onLoadBill(CongressException exception) {
		this.loadBillTask = null;
		if (exception instanceof CongressException.NotFound)
			Utils.alert(this, R.string.bill_loading_error);
		else
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
	public void addBillTimeline(LinearLayout header) {
		LinearLayout inner = (LinearLayout) header.findViewById(R.id.header_inner);
		
		addTimelinePiece(inner, "Introduced on", introduced_at);
		
		if (house_result != null && house_result_at > 0) {
			if (house_result.equals("pass"))
				addTimelinePiece(inner, "Passed the House on", house_result_at);
			else if (house_result.equals("fail"))
				addTimelinePiece(inner, "Failed the House on", house_result_at);
		}
		
		if (senate_result != null && senate_result_at > 0) {
			if (senate_result.equals("pass"))
				addTimelinePiece(inner, "Passed the Senate on", senate_result_at);
			else if (senate_result.equals("fail"))
				addTimelinePiece(inner, "Failed the Senate on", senate_result_at);
		}
		
		if (awaiting_signature && awaiting_signature_since > 0)
			addTimelinePiece(inner, "Awaiting signature since", awaiting_signature_since);
		
		if (enacted && enacted_at > 0)
			addTimelinePiece(inner, "Enacted on", enacted_at);
	}
	
	public void addTimelinePiece(LinearLayout container, String prefix, long timestamp) {
		String date = prefix + " " + timelineFormat.format(new Date(timestamp));
		TextView piece = (TextView) LayoutInflater.from(this).inflate(R.layout.bill_event, null);
		piece.setText(date);
		container.addView(piece);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		String type = (String) v.getTag();
    	if (type.equals("sponsor") && sponsor_id != null)
    		startActivity(Utils.legislatorIntent(sponsor_id));
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
    		sendBroadcast(Utils.shortcutIntent(this, id, code)
    				.setAction("com.android.launcher.action.INSTALL_SHORTCUT"));
    		break;
    	case R.id.thomas:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.thomasUrl(type, number, session))));
    		break;
    	case R.id.govtrack:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.govTrackUrl(type, number, session))));
    		break;
    	case R.id.opencongress:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.openCongressUrl(type, number, session))));
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
		String summary;
		
		public BillInfoHolder(LoadBillTask loadBillTask, LoadPhotoTask loadPhotoTask, String summary) {
			this.loadBillTask = loadBillTask;
			this.loadPhotoTask = loadPhotoTask;
			this.summary = summary;
		}
	}
}