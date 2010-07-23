package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

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
	
	private LoadingWrapper lw;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled);

		Bundle extras = getIntent().getExtras();
		type = extras.getInt("type", BILLS_RECENT);
		sponsor_id = extras.getString("sponsor_id");
		sponsor_name = extras.getString("sponsor_name");

		setupControls();

		BillListHolder holder = (BillListHolder) getLastNonConfigurationInstance();

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
		return new BillListHolder(bills, loadBillsTask);
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
			Utils.setTitle(this, "Latest Bills by\n" + sponsor_name, R.drawable.bill_multiple);
			Utils.setTitleSize(this, 18);
			break;
		case BILLS_LATEST_VOTES:
			Utils.setTitle(this, R.string.menu_bills_latest_votes, R.drawable.bill_vote);
			break;
		}
	}

	protected void onListItemClick(ListView parent, View v, int position, long id) {
		Bill bill = (Bill) parent.getItemAtPosition(position);
		if (bill != null)
			startActivity(Utils.billIntent(this, bill));
	}

	public void loadBills() {
		if (loadBillsTask == null)
			loadBillsTask = (LoadBillsTask) new LoadBillsTask(this).execute();
	}


	public void onLoadBills(ArrayList<Bill> newBills) {
		if (bills.size() == 0 && newBills.size() == 0) {
			if (type == BILLS_SPONSOR)
				Utils.showBack(this, R.string.empty_bills_sponsored);
			else
				Utils.showBack(this, R.string.empty_bills);
			return;
		}
		
		// remove the placeholder and add the new bills in the array
		if (bills.size() > 0) {
			int lastIndex = bills.size() - 1;
			if (bills.get(lastIndex) == null) {
				bills.remove(lastIndex);
			}
		}

		bills.addAll(newBills);

		// if we got back a full page of bills, there may be more yet to come
		if (newBills.size() == BILLS)
			bills.add(null);

		((BillAdapter) getListAdapter()).notifyDataSetChanged();
	}

	public void onLoadBills(CongressException exception) {
		if (bills.size() > 0) {
			
			lw.getLoading().setVisibility(View.GONE);
			lw.getRetryContainer().setVisibility(View.VISIBLE);
			
			Button retry = lw.getRetry();
			retry.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					lw.getRetryContainer().setVisibility(View.GONE);
					lw.getLoading().setVisibility(View.VISIBLE);
					loadBills();
				}
			});

		} else
			Utils.showBack(this, R.string.error_connection);
	}

	private class LoadBillsTask extends AsyncTask<Void,Void,ArrayList<Bill>> {
		private BillList context;
		private CongressException exception;

		public LoadBillsTask(BillList context) {
			this.context = context;
			Utils.setupDrumbone(context);
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
					return BillService.recentlyIntroduced(BILLS, page);
				case BILLS_LAW:
					return BillService.recentLaws(BILLS, page);
				case BILLS_LATEST_VOTES:
					return BillService.latestVotes(BILLS, page);
				case BILLS_SPONSOR:
					return BillService.recentlySponsored(BILLS, context.sponsor_id, page);
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

	private static class BillAdapter extends ArrayAdapter<Bill> {
		private LayoutInflater inflater;
		private BillList context;
		
		private static final int BILL = 0;
		private static final int LOADING = 1;

		public BillAdapter(BillList context, ArrayList<Bill> bills) {
			super(context, 0, bills);
			this.inflater = LayoutInflater.from(context);
			this.context = context;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return !((position == getCount() - 1) && getItem(position) == null);
		}
		
		@Override
		public int getItemViewType(int position) {
			if (getItem(position) != null)
				return BILL;
			else
				return LOADING;
		}
		
		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Bill bill = getItem(position);

			if (bill == null)
				return getLoadingView();
			else
				return getBillView(bill, convertView);
		}

		private View getLoadingView() {
			context.loadBills();
			context.lw = new LoadingWrapper(inflater.inflate(R.layout.loading_retry, null));
			return context.lw.getBase();
		}

		private View getBillView(Bill bill, View view) {
			ViewHolder holder;
			if (view == null) {
				view = inflater.inflate(R.layout.bill_item, null);
				
				holder = new ViewHolder();
				holder.byline = (TextView) view.findViewById(R.id.byline);
				holder.date = (TextView) view.findViewById(R.id.date);
				holder.title = (TextView) view.findViewById(R.id.title);
				
				view.setTag(holder);
			} else
				holder = (ViewHolder) view.getTag();
			
			String code, action;
			Date date = null;
			switch (context.type) {
			case BILLS_LAW:
				code = Bill.formatCode(bill.code);
				date = bill.enacted_at;
				action = "became law";
				break;
			case BILLS_LATEST_VOTES:
				code = Bill.formatCodeShort(bill.code);
				date = bill.last_vote_at;
				action = (bill.last_vote_result.equals("pass") ? "passed the " : "failed in the ") + Utils.capitalize(bill.last_vote_chamber);
				break;
			case BILLS_RECENT:
			case BILLS_SPONSOR:
			default:
				code = Bill.formatCode(bill.code);
				date = bill.introduced_at;
				action = "was introduced";
				break;
			}
			Spanned byline = Html.fromHtml("<b>" + code + "</b> " + action + ":");
			holder.byline.setText(byline);

			if (date != null) {
				SimpleDateFormat format = null;
				if(date.getYear() == new Date().getYear()) 
					format = new SimpleDateFormat("MMM dd");
				else
					format = new SimpleDateFormat("MMM dd, yyyy");
				holder.date.setText(format.format(date));
			}

			if (bill.short_title != null) {
				String title = Utils.truncate(bill.short_title, 300);
				holder.title.setTextSize(19);
				holder.title.setText(title);
			} else { // if (bill.official_title != null)
				String title = Utils.truncate(bill.official_title, 300);
				holder.title.setTextSize(16);
				holder.title.setText(title);
			}

			return view;
		}
		
		static class ViewHolder {
			TextView byline, date, title;
		}
	}

	static class BillListHolder {
		ArrayList<Bill> bills;
		LoadBillsTask loadBillsTask;

		public BillListHolder(ArrayList<Bill> bills, LoadBillsTask loadBillsTask) {
			this.bills = bills;
			this.loadBillsTask = loadBillsTask;
		}
	}
	
	static class LoadingWrapper {
		private View base, loading, retryContainer;
		private Button retry;

		public LoadingWrapper(View base) {
			this.base = base;
		}
		public View getLoading() {
			return loading == null ? loading = base.findViewById(R.id.loading_layout) : loading;
		}
		public Button getRetry() {
			return retry == null ? retry = (Button) base.findViewById(R.id.retry) : retry;
		}
		
		public View getRetryContainer() {
			return retryContainer == null ? retryContainer = base.findViewById(R.id.retry_container) : retryContainer;
		}
		
		public View getBase() {
			return base;
		}
	}
}