package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;

public class BillList extends ListActivity {
	private static final int BILLS = 20;
	
	public static final int BILLS_LAW = 0;
	public static final int BILLS_RECENT = 1;
	public static final int BILLS_SPONSOR = 2;
	public static final int BILLS_LATEST_VOTES = 3;
	
	private ArrayList<Bill> bills;
	private LoadBillsTask loadBillsTask;
	
	private String sponsor_id, sponsor_name;
	private int type;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled);
		
		Utils.setupDrumbone(this);
		
		Bundle extras = getIntent().getExtras();
		type = extras.getInt("type", BILLS_RECENT);
		sponsor_id = extras.getString("sponsor_id");
		sponsor_name = extras.getString("sponsor_name");
		
		setupControls();

		MainActivityHolder holder = (MainActivityHolder) getLastNonConfigurationInstance();

		if (holder != null) {
			this.bills = holder.bills;
			this.loadBillsTask = holder.loadBillsTask;

			if (loadBillsTask != null)
				loadBillsTask.onScreenLoad(this);
		} else
			bills = new ArrayList<Bill>();
		
		setListAdapter(new BillAdapter(this, bills));
		
		if (bills.size() == 0)
			loadBills();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new MainActivityHolder(bills, loadBillsTask);
	}
	
	public void setupControls() {
		((Button) findViewById(R.id.back)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		Utils.setLoading(this, R.string.bills_loading);
		switch (type) {
		case BILLS_RECENT:
			Utils.setTitle(this, R.string.menu_bills_recent, R.drawable.bill_recent);
			break;
		case BILLS_LAW:
			Utils.setTitle(this, R.string.menu_bills_law, R.drawable.bill_law);
			break;
		case BILLS_SPONSOR:
			Utils.setTitle(this, "Latest Bills by " + sponsor_name, R.drawable.bill_multiple);
			Utils.setTitleSize(this, 20);
			break;
		case BILLS_LATEST_VOTES:
			Utils.setTitle(this, R.string.menu_bills_latest_votes, R.drawable.bill_recent);
			break;
		}
	}
	
	protected void onListItemClick(ListView parent, View v, int position, long id) {
    	Bill bill = (Bill) parent.getItemAtPosition(position);
		if (bill != null)
			startActivity(Utils.billIntentExtra(this, bill));
    }
	
	public void loadBills() {
		if (loadBillsTask == null)
			loadBillsTask = (LoadBillsTask) new LoadBillsTask(this).execute();
	}
	

	public void onLoadBills(ArrayList<Bill> bills) {
		// remove the placeholder and add the new bills in the array
		if (this.bills.size() > 0) {
			int lastIndex = this.bills.size() - 1;
			if (this.bills.get(lastIndex) == null) {
				this.bills.remove(lastIndex);
			}
		}
		
		this.bills.addAll(bills);

		// if we got back a full page of bills, there may be more yet to come
		if (bills.size() == BILLS)
			this.bills.add(null);

		((BillAdapter) getListAdapter()).notifyDataSetChanged();
	}
	
	public void onLoadBills(CongressException exception) {
		this.bills = new ArrayList<Bill>();
		Utils.showBack(this, R.string.error_connection);
	}

	private class LoadBillsTask extends AsyncTask<Void,Void,ArrayList<Bill>> {
		private BillList context;
		private CongressException exception;
		
		public LoadBillsTask(BillList context) {
			this.context = context;
		}
		
		public void onScreenLoad(BillList context) {
			this.context = context;
		}
		
		@Override
		public ArrayList<Bill> doInBackground(Void... nothing) {
			try {
				int page = (context.bills.size() / BILLS) + 1;

				switch (context.type) {
				case BILLS_RECENT:
					return Bill.recentlyIntroduced(BILLS, page);
				case BILLS_LAW:
					return Bill.recentLaws(BILLS, page);
				case BILLS_LATEST_VOTES:
					return Bill.latestVotes(BILLS, page);
				case BILLS_SPONSOR:
					return Bill.recentlySponsored(BILLS, context.sponsor_id, page);
				default:
					throw new CongressException("Not sure what type of bills to find.");
				}
			} catch(CongressException exception) {
				this.exception = exception;
				return null;
			}
		}
		
		@Override
		public void onPostExecute(ArrayList<Bill> bills) {
			context.loadBillsTask = null;
			
			if (exception != null)
				context.onLoadBills(exception);
			else
				context.onLoadBills(bills);
		}
	}
	
	private class BillAdapter extends ArrayAdapter<Bill> {
		LayoutInflater inflater;

	    public BillAdapter(Activity context, ArrayList<Bill> bills) {
	        super(context, 0, bills);
	        inflater = LayoutInflater.from(context);
	    }

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return !((position == bills.size() - 1) && bills.get(position) == null);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Bill bill = getItem(position);
			if (bill == null)
				return getLoadMoreView();
			else
				return getBillView(bill, convertView);
		}

		private View getLoadMoreView() {
			BillList.this.loadBills();
			return inflater.inflate(R.layout.loading, null);
		}

		private View getBillView(Bill bill, View convertView) {
			RelativeLayout view;
			if (convertView == null || !(convertView instanceof RelativeLayout))
				view = (RelativeLayout) inflater.inflate(R.layout.bill_item, null);
			else
				view = (RelativeLayout) convertView;
			
			String code = Bill.formatCode(bill.code);
			String action;
			Date date = null;
			switch (type) {
			case BILLS_LAW:
				date = bill.enacted_at;
				action = "became law";
				break;
			case BILLS_LATEST_VOTES:
				date = bill.last_vote_at;
				action = (bill.last_vote_result.equals("pass") ? "passed the " : "failed in the ") + Utils.capitalize(bill.last_vote_chamber);
				break;
			case BILLS_RECENT:
			case BILLS_SPONSOR:
			default:
				date = bill.introduced_at;
				action = "was introduced";
				break;
			}
			Spanned byline = Html.fromHtml("<b>" + code + "</b> " + action + ":");
			((TextView) view.findViewById(R.id.byline)).setText(byline);
			
			if(date != null) {
				SimpleDateFormat format = null;
				if(date.getYear() == new Date().getYear()) 
					format = new SimpleDateFormat("MMM dd");
				else
					format = new SimpleDateFormat("MMM dd, yyyy");
				((TextView) view.findViewById(R.id.date)).setText(format.format(date));
			}
				
			TextView titleView = ((TextView) view.findViewById(R.id.title));
			if (bill.short_title != null) {
				String title = Utils.truncate(bill.short_title, 300);
				titleView.setTextSize(19);
				titleView.setText(title);
			} else { // if (bill.official_title != null)
				String title = Utils.truncate(bill.official_title, 300);
				titleView.setTextSize(16);
				titleView.setText(title);
			} 
			
			view.setTag(bill);
			
			return view;
		}
	}
	
	static class MainActivityHolder {
		ArrayList<Bill> bills;
		LoadBillsTask loadBillsTask;

		public MainActivityHolder(ArrayList<Bill> bills, LoadBillsTask loadBillsTask) {
			this.bills = bills;
			this.loadBillsTask = loadBillsTask;
		}
	}
}