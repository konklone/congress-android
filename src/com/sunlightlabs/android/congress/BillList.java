package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.BillService;

public class BillList extends ListActivity {
	public static final int PER_PAGE = 20;

	public static final int BILLS_LAW = 0;
	public static final int BILLS_RECENT = 1;
	public static final int BILLS_SPONSOR = 2;

	private List<Bill> bills;
	private LoadBillsTask loadBillsTask;

	private Legislator sponsor;
	private int type;
	
	private Footer footer;
	private LoadingWrapper lw;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer_titled);

		Bundle extras = getIntent().getExtras();
		type = extras.getInt("type", BILLS_RECENT);
		sponsor = (Legislator) extras.getSerializable("legislator");

		setupControls();

		BillListHolder holder = (BillListHolder) getLastNonConfigurationInstance();

		if (holder != null) {
			this.bills = holder.bills;
			this.loadBillsTask = holder.loadBillsTask;
			this.footer = holder.footer;

			if (loadBillsTask != null)
				loadBillsTask.onScreenLoad(this);
		} else
			bills = new ArrayList<Bill>();

		if (footer != null)
			footer.onScreenLoad(this);
		else
			footer = Footer.from(this);
		
		setListAdapter(new BillAdapter(this, bills));

		if (bills.size() == 0)
			loadBills();
		else
			setupSubscription();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new BillListHolder(bills, loadBillsTask, footer);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (bills != null && bills.size() > 0)
			setupSubscription();
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
			Utils.setTitle(this, "Latest Bills by\n" + sponsor.titledName(), R.drawable.bill_multiple);
			Utils.setTitleSize(this, 18);
			break;
		}
	}
	
	private void setupSubscription() {
		Subscription subscription = null;
		if (type == BILLS_RECENT)
			subscription = new Subscription("RecentBills", "Introduced Bills", "BillsRecentSubscriber", null);
		else if (type == BILLS_SPONSOR)
			subscription = new Subscription(sponsor.id, Subscriber.notificationName(sponsor), "BillsLegislatorSubscriber", null);
		else if (type == BILLS_LAW)
			subscription = new Subscription("RecentLaws", "New Laws", "BillsLawsSubscriber", null); 
		
		footer.init(subscription, bills);
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


	public void onLoadBills(List<Bill> newBills) {
		// if this is the first page of rolls, set up the subscription
		if (bills.size() == 0) {
			if (newBills.size() == 0) {
				if (type == BILLS_SPONSOR)
					Utils.showBack(this, R.string.empty_bills_sponsored);
				else
					Utils.showBack(this, R.string.empty_bills);
				setupSubscription();
				return;
			}
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
		if (newBills.size() == PER_PAGE)
			bills.add(null);

		((BillAdapter) getListAdapter()).notifyDataSetChanged();
		
		setupSubscription();
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

	private class LoadBillsTask extends AsyncTask<Void,Void,List<Bill>> {
		private BillList context;
		private CongressException exception;

		public LoadBillsTask(BillList context) {
			this.context = context;
			Utils.setupRTC(context);
		}

		public void onScreenLoad(BillList context) {
			this.context = context;
		}

		@Override
		public List<Bill> doInBackground(Void... nothing) {
			try {
				int page = (context.bills.size() / PER_PAGE) + 1;

				switch (context.type) {
				case BILLS_RECENT:
					return BillService.recentlyIntroduced(page, PER_PAGE);
				case BILLS_LAW:
					return BillService.recentLaws(page, PER_PAGE);
				case BILLS_SPONSOR:
					return BillService.recentlySponsored(sponsor.id, page, PER_PAGE);
				default:
					throw new CongressException("Not sure what type of bills to find.");
				}
			} catch(CongressException exception) {
				this.exception = exception;
				return null;
			}
		}

		@Override
		public void onPostExecute(List<Bill> bills) {
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

		public BillAdapter(BillList context, List<Bill> bills) {
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
			} else if (bill.official_title != null) {
				String title = Utils.truncate(bill.official_title, 300);
				holder.title.setTextSize(16);
				holder.title.setText(title);
			} else {
				holder.title.setTextSize(16);
				if (bill.abbreviated)
					holder.title.setText(R.string.bill_no_title_yet);
				else
					holder.title.setText(R.string.bill_no_title);
			}

			return view;
		}
		
		static class ViewHolder {
			TextView byline, date, title;
		}
	}

	static class BillListHolder {
		List<Bill> bills;
		LoadBillsTask loadBillsTask;
		Footer footer;

		public BillListHolder(List<Bill> bills, LoadBillsTask loadBillsTask, Footer footer) {
			this.bills = bills;
			this.loadBillsTask = loadBillsTask;
			this.footer = footer;
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