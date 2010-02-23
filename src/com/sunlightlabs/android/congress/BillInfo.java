package com.sunlightlabs.android.congress;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Legislator;
import com.sunlightlabs.entities.Committee;

public class BillInfo extends ListActivity {
	private String id, code, title;
	private Time introduced_at;
	
	private Bill bill;
	private LoadBillTask loadBillTask;
	private LinearLayout header;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		code = extras.getString("code");
		title = extras.getString("title");
		introduced_at = new Time();
		introduced_at.set(extras.getLong("introduced_at"));
		
		setupControls();
		
		BillInfoHolder holder = (BillInfoHolder) getLastNonConfigurationInstance();
        if (holder != null) {
        	bill = holder.bill;
        	loadBillTask = holder.loadBillTask;
        }
		
		loadBill();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new BillInfoHolder(bill, loadBillTask);
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
	
	public void onLoadBill(Bill bill) {
		this.bill = bill;
		displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		Utils.alert(this, exception);
		finish();
	}
	
	public void setupControls() {
		LayoutInflater inflater = LayoutInflater.from(this);
		header = (LinearLayout) inflater.inflate(R.layout.bill_header, null);
		((TextView) header.findViewById(R.id.code)).setText(Bill.formatCode(code));
		TextView titleView = (TextView) header.findViewById(R.id.title);
		String truncated = truncateTitle(title);
		titleView.setText(truncated);
		titleView.setTextSize(sizeOfTitle(truncated));
		
		((TextView) header.findViewById(R.id.loading_message)).setText("Loading bill details...");
		
		MergeAdapter adapter = new MergeAdapter();
		adapter.addView(header);
		setListAdapter(adapter);
	}
	
	public void displayBill() {
		LayoutInflater inflater = LayoutInflater.from(this);
		
		header.findViewById(R.id.loading).setVisibility(View.GONE);
		
		LinearLayout sponsorHeader = (LinearLayout) inflater.inflate(R.layout.header_layout_small, null);
		((TextView) sponsorHeader.findViewById(R.id.header_text)).setText("Sponsor");
		
		LinearLayout sponsor = (LinearLayout) inflater.inflate(R.layout.legislator_item, null);
		((TextView) sponsor.findViewById(R.id.name)).setText(nameFor(bill.sponsor));
		((TextView) sponsor.findViewById(R.id.position)).setText(positionFor(bill.sponsor));
		sponsor.setTag("sponsor");
		ArrayList<View> sponsorViews = new ArrayList<View>(1);
		sponsorViews.add(sponsor);
		
		MergeAdapter adapter = (MergeAdapter) getListAdapter();
		adapter.addView(sponsorHeader);
		adapter.addAdapter(new ViewArrayAdapter(this, sponsorViews));
		setListAdapter(adapter);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		String type = (String) v.getTag();
    	if (type.equals("sponsor"))
    		startActivity(Utils.legislatorIntent(bill.sponsor.bioguide_id));
    }
	
	
	public String nameFor(Legislator legislator) {
		return legislator.last_name + ", " + legislator.firstName();
	}
	
	public String positionFor(Legislator legislator) {
		String district = legislator.district;
		String stateName = Utils.stateCodeToName(this, legislator.state);
		
		if (district.equals("Senior Seat"))
			return "Senior Senator from " + stateName;
		else if (district.equals("Junior Seat"))
			return "Junior Senator from " + stateName;
		else if (district.equals("0")) {
			if (legislator.title.equals("Rep"))
				return "Representative for " + stateName + " At-Large";
			else
				return legislator.fullTitle() + " for " + stateName;
		} else
			return "Representative for " + stateName + "-" + district;
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
	
	public String truncateTitle(String title) {
		if (title.length() > 400)
			return Utils.truncate(title, 400);
		else
			return title;
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
				Bill bill = Bill.find(billId[0], "basic,extended,sponsor");
				
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
		LoadBillTask loadBillTask;
		Bill bill;
		
		public BillInfoHolder(Bill bill, LoadBillTask loadBillTask) {
			this.bill = bill;
			this.loadBillTask = loadBillTask;
		}
	}
}