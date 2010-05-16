package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
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
import com.sunlightlabs.android.congress.utils.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.LoadsPhoto;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Legislator;

public class BillInfo extends ListActivity implements LoadsPhoto {
	private String id;
	private boolean extra;
	
	// fields which may come either in the extras of the Intent, or need to be fetched remotely
	private String code, short_title, official_title;
	private boolean passed, vetoed, awaiting_signature, enacted;
	private String house_result, senate_result, override_house_result, override_senate_result;
	private long introduced_at, house_result_at, senate_result_at, passed_at;
	private long vetoed_at, override_house_result_at, override_senate_result_at;
	private long awaiting_signature_since, enacted_at;
	
	private String sponsor_id, sponsor_party, sponsor_state, sponsor_title;
	private String sponsor_first_name, sponsor_last_name, sponsor_nickname;
	
	private Bill bill;
	private LoadBillTask loadBillTask;
	private LoadPhotoTask loadPhotoTask;
	private LinearLayout loadingContainer, sponsorView;
	
	private Drawable sponsorPhoto;
	
	private SimpleDateFormat timelineFormat = new SimpleDateFormat("MMM dd, yyyy");
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.loading_fullscreen);
		
		Utils.setupDrumbone(this);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		extra = extras.getBoolean("extra", false);
		
		setupControls();
		
		BillInfoHolder holder = (BillInfoHolder) getLastNonConfigurationInstance();
        if (holder != null) {
        	bill = holder.bill;
        	loadBillTask = holder.loadBillTask;
        	loadPhotoTask = holder.loadPhotoTask;
        }
		
		loadBill();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new BillInfoHolder(bill, loadBillTask, loadPhotoTask);
	}
	
	
	public void loadBill() {
		if (loadBillTask != null)
			loadBillTask.onScreenLoad(this);
		else {
			if (bill != null)
				displayBill();
			else
				loadBillTask = (LoadBillTask) new LoadBillTask(this).execute(id);
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
		this.bill = bill;
		displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		Utils.alert(this, R.string.error_connection);
		finish();
	}
	
	public void onLoadPhoto(Drawable photo, Object tag) {
		sponsorPhoto = photo;
		loadPhotoTask = null;
		displayPhoto();
	}
	
	public Context photoContext() {
		return this;
	}
	
	
	public void displayPhoto() {
		if (sponsorPhoto != null && sponsorView != null)
    		((ImageView) sponsorView.findViewById(R.id.picture)).setImageDrawable(sponsorPhoto);
	}
	
	public void setupControls() {
		MergeAdapter adapter = new MergeAdapter();
		
		if (extra) {
			Bundle extras = getIntent().getExtras();
			code = extras.getString("code");
			short_title = extras.getString("short_title");
			official_title = extras.getString("official_title");
			
			// my god
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
			
			loadingContainer = displayBillBasic(adapter);
			((TextView) loadingContainer.findViewById(R.id.loading_message)).setText("Loading bill details...");
			loadingContainer.findViewById(R.id.loading).setVisibility(View.VISIBLE);
		} else
			((TextView) findViewById(R.id.loading_message)).setText("Loading bill details...");
		
		setProgressBarIndeterminateVisibility(true);
		
		setListAdapter(adapter);
	}
	
	public void displayBill() {
		LayoutInflater inflater = LayoutInflater.from(this);
		MergeAdapter adapter;
		
		Legislator sponsor = bill.sponsor;
		
		if (extra) {
			adapter = (MergeAdapter) getListAdapter();
		} else {
			adapter = new MergeAdapter();
			
			code = bill.code;
			short_title = bill.short_title;
			official_title = bill.official_title;
			
			house_result = bill.house_result;
			senate_result = bill.senate_result;
			passed = bill.passed;
			vetoed = bill.vetoed;
			override_house_result = bill.override_house_result;
			override_senate_result = bill.override_senate_result;
			awaiting_signature = bill.awaiting_signature;
			enacted = bill.enacted;
			
			introduced_at = bill.introduced_at.getTime();
			if (bill.house_result_at != null)
				house_result_at = bill.house_result_at.getTime();
			if (bill.senate_result_at != null)
				senate_result_at = bill.senate_result_at.getTime();
			if (bill.passed_at != null)
				passed_at = bill.passed_at.getTime();
			if (bill.vetoed_at != null)
				vetoed_at = bill.vetoed_at.getTime();
			if (bill.override_house_result_at != null)
				override_house_result_at = bill.override_house_result_at.getTime();
			if (bill.override_senate_result_at != null)
				override_senate_result_at = bill.override_senate_result_at.getTime();
			if (bill.awaiting_signature_since != null)
				awaiting_signature_since = bill.awaiting_signature_since.getTime();
			if (bill.enacted_at != null)
				enacted_at = bill.enacted_at.getTime();
			
			if (sponsor != null) {
				sponsor_id = sponsor.bioguide_id;
				sponsor_title = sponsor.title;
				sponsor_state = sponsor.state;
				sponsor_party = sponsor.party;
				sponsor_first_name = sponsor.first_name;
				sponsor_nickname = sponsor.nickname;
				sponsor_last_name = sponsor.last_name;
			}
			
			loadingContainer = displayBillBasic(adapter);
		}
		loadingContainer.findViewById(R.id.loading).setVisibility(View.GONE);
		setProgressBarIndeterminateVisibility(false);
		
		// handle all the additional bill data
		LinearLayout summary; 
		if (bill.summary != null && bill.summary.length() > 0) {
			summary = (LinearLayout) inflater.inflate(R.layout.bill_summary, null);
			String formatted = Bill.formatSummary(bill.summary, bill.short_title);
			((TextView) summary.findViewById(R.id.summary)).setText(formatted);
		} else {
			summary = (LinearLayout) inflater.inflate(R.layout.bill_no_summary, null);
			TextView noSummary = (TextView) summary.findViewById(R.id.no_summary);
			noSummary.setText(Html.fromHtml("No summary available.<br/><br/><a href=\"" + Bill.thomasUrl(bill) + "\">Read the text of this bill on THOMAS.</a>"));
        	noSummary.setMovementMethod(LinkMovementMethod.getInstance());
		}
		adapter.addView(summary);
		
		setListAdapter(adapter);
	}
	
	public LinearLayout displayBillBasic(MergeAdapter adapter) {
		LayoutInflater inflater = LayoutInflater.from(this);
		LinearLayout header = (LinearLayout) inflater.inflate(R.layout.bill_header, null);
		
		((TextView) header.findViewById(R.id.title_text)).setText(Bill.formatCode(code));
		
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
		
		LinearLayout summaryHeader = (LinearLayout) inflater.inflate(R.layout.header_loading, null);
		((TextView) summaryHeader.findViewById(R.id.header_text)).setText("Summary");
		adapter.addView(summaryHeader);
		
		return summaryHeader;
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (bill != null) {
			menu.findItem(R.id.shortcut).setEnabled(true);
			menu.findItem(R.id.thomas).setEnabled(true);
			menu.findItem(R.id.govtrack).setEnabled(true);
			menu.findItem(R.id.opencongress).setEnabled(true);
		}
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.main:
    		startActivity(new Intent(this, MainMenu.class));
    		break;
    	case R.id.shortcut:
    		sendBroadcast(Utils.shortcutIntent(this, bill)
    				.setAction("com.android.launcher.action.INSTALL_SHORTCUT"));
    		break;
    	case R.id.thomas:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.thomasUrl(bill))));
    		break;
    	case R.id.govtrack:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.govTrackUrl(bill))));
    		break;
    	case R.id.opencongress:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.openCongressUrl(bill))));
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
	
	private class LoadBillTask extends AsyncTask<String,Void,Bill> {
		private BillInfo context;
		private CongressException exception;
		
		public LoadBillTask(BillInfo context) {
			this.context = context;
		}
		
		public void onScreenLoad(BillInfo context) {
			this.context = context;
		}
		
		@Override
		public Bill doInBackground(String... billId) {
			try {
				Bill bill = Bill.find(billId[0], "basic,sponsor,summary");
				
				return bill;
			} catch (CongressException exception) {
				this.exception = exception;
				return null;
			}
		}
		
		@Override
		public void onPostExecute(Bill bill) {
			context.loadBillTask = null;
			
			if (exception != null && bill == null)
				context.onLoadBill(exception);
			else
				context.onLoadBill(bill);
		}
	}
	
	static class BillInfoHolder {
		Bill bill;
		LoadBillTask loadBillTask;
		LoadPhotoTask loadPhotoTask;
		
		public BillInfoHolder(Bill bill, LoadBillTask loadBillTask, LoadPhotoTask loadPhotoTask) {
			this.bill = bill;
			this.loadBillTask = loadBillTask;
			this.loadPhotoTask = loadPhotoTask;
		}
	}
}