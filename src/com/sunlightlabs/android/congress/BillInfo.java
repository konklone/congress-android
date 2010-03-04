package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.ListActivity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Drumbone;
import com.sunlightlabs.congress.java.Legislator;

public class BillInfo extends ListActivity {
	private String id;
	private boolean extra;
	
	// fields which may come either in the extras of the Intent, or need to be fetched remotely
	private String code, short_title, official_title;
	private long introduced_at, enacted_at;
	private String sponsor_id, sponsor_party, sponsor_state, sponsor_title;
	private String sponsor_first_name, sponsor_last_name, sponsor_nickname;
	
	private Bill bill;
	private LoadBillTask loadBillTask;
	private LoadPhotoTask loadPhotoTask;
	private LinearLayout loadingContainer, sponsorView;
	
	private Drawable sponsorPhoto;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.loading_fullscreen);
		
		Drumbone.apiKey = getResources().getString(R.string.sunlight_api_key);
		Drumbone.baseUrl = getResources().getString(R.string.drumbone_base_url);
		
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
        		loadPhotoTask = (LoadPhotoTask) new LoadPhotoTask(this).execute(sponsor_id);
        }
	}
	
	public void onLoadBill(Bill bill) {
		this.bill = bill;
		displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		Utils.alert(this, exception);
		finish();
	}
	
	public void onLoadPhoto(Drawable photo) {
		sponsorPhoto = photo;
		loadPhotoTask = null;
		displayPhoto();
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
			introduced_at = extras.getLong("introduced_at", 0);
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
			introduced_at = bill.introduced_at.getTime();
			
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
		} else
			summary = (LinearLayout) inflater.inflate(R.layout.bill_no_summary, null);
		adapter.addView(summary);
		
		setListAdapter(adapter);
	}
	
	public LinearLayout displayBillBasic(MergeAdapter adapter) {
		String displayCode = Bill.formatCode(code);
		String appName = getResources().getString(R.string.app_name);
		setTitle(appName + " - " + displayCode);
		
		LayoutInflater inflater = LayoutInflater.from(this);
		LinearLayout header = (LinearLayout) inflater.inflate(R.layout.bill_header, null);
		((TextView) header.findViewById(R.id.code)).setText(displayCode);
		
		TextView titleView = (TextView) header.findViewById(R.id.title);
		String title;
		if (short_title != null) {
			title = Utils.truncate(short_title, 400);
			titleView.setTextSize(19);
		} else {
			title = official_title;
			titleView.setTextSize(16);
		}
		titleView.setText(title);
		
		String date = "Introduced on " + new SimpleDateFormat("MMM dd, yyyy").format(new Date(introduced_at));
		((TextView) header.findViewById(R.id.introduced)).setText(date);
		
		if (enacted_at > 0) {
			TextView enacted = (TextView) header.findViewById(R.id.enacted);
			enacted.setText("Enacted on " + new SimpleDateFormat("MMM dd, yyyy").format(new Date(enacted_at)));
			enacted.setVisibility(View.VISIBLE);
		}
		
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
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		String type = (String) v.getTag();
    	if (type.equals("sponsor") && sponsor_id != null)
    		startActivity(Utils.legislatorIntent(sponsor_id));
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
				Bill bill = Bill.find(billId[0], "basic,extended,sponsor,summary");
				
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
	
	private class LoadPhotoTask extends AsyncTask<String,Void,Drawable> {
		public BillInfo context;
		
		public LoadPhotoTask(BillInfo context) {
			super();
			this.context = context;
		}
		
		public void onScreenLoad(BillInfo context) {
			this.context = context;
		}
		
		@Override
		public Drawable doInBackground(String... bioguideId) {
			return LegislatorImage.getImage(LegislatorImage.PIC_LARGE, bioguideId[0], context);
		}
		
		@Override
		public void onPostExecute(Drawable photo) {
			context.onLoadPhoto(photo);
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