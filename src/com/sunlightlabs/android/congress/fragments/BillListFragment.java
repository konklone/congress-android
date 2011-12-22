package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.BillService;

public class BillListFragment extends ListFragment {
	
	public static final int PER_PAGE = 20;
	
	public static final int BILLS_LAW = 0;
	public static final int BILLS_RECENT = 1;
	public static final int BILLS_SPONSOR = 2;
	public static final int BILLS_SEARCH_NEWEST = 3;
	public static final int BILLS_SEARCH_RELEVANT = 4;
	public static final int BILLS_CODE = 5;
	
	List<Bill> bills;
	
	int type;
	Legislator sponsor;
	String code;
	String query;
	
	public static BillListFragment forRecent() {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", BILLS_RECENT);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static BillListFragment forLaws() {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", BILLS_LAW);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static BillListFragment forSponsor(Legislator sponsor) {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", BILLS_SPONSOR);
		args.putSerializable("sponsor", sponsor);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static BillListFragment forCode(String code) {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", BILLS_CODE);
		args.putString("code", code);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static BillListFragment forSearch(String query, int type) {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", type);
		args.putString("query", query);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public BillListFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		type = args.getInt("type");
		query = args.getString("query");
		code = args.getString("code");
		sponsor = (Legislator) args.getSerializable("sponsor");
		
		loadBills();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_footer, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (bills != null)
			displayBills();
	}
	
	public void setupControls() {
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});

		FragmentUtils.setLoading(this, R.string.bills_loading);
	}
	
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		startActivity(Utils.billIntent(getActivity(), (Bill) parent.getItemAtPosition(position)));
	}
	
//	public String url() {
//		if (type == BILLS_RECENT)
//			return "/bills/introduced";
//		else if (type == BILLS_SPONSOR)
//			return "/legislator/bills";
//		else if (type == BILLS_LAW)
//			return "/bills/laws";
//		else if (type == BILLS_SEARCH)
//			return "/bills/search/newest";
//		else if (type == BILLS_CODE)
//			return "/bills/search/code";
//		else
//			return "/bills";
//	}
	
	private void refresh() {
		bills = null;
		FragmentUtils.setLoading(this, R.string.bills_loading);
		FragmentUtils.showLoading(this);
		loadBills();
	}
	
	public void loadBills() {
		new LoadBillsTask(this).execute();
	}
	
	public void onLoadBills(List<Bill> bills) {
		this.bills = bills;
		if (isAdded())
			displayBills();
	}
	
	public void onLoadBills(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.bills_error);
	}
	
	public void displayBills() {
		if (bills.size() > 0) {
			setListAdapter(new BillAdapter(this, bills));
			setupSubscription();
		} else
			FragmentUtils.showRefresh(this, R.string.bills_error); // should not happen
	}
	
	private void setupSubscription() {
		Subscription subscription = null;
		if (type == BILLS_RECENT)
			subscription = new Subscription("RecentBills", "Introduced Bills", "BillsRecentSubscriber", null);
		else if (type == BILLS_SPONSOR)
			subscription = new Subscription(sponsor.id, Subscriber.notificationName(sponsor), "BillsLegislatorSubscriber", null);
		else if (type == BILLS_LAW)
			subscription = new Subscription("RecentLaws", "New Laws", "BillsLawsSubscriber", null);
		else if (type == BILLS_SEARCH_NEWEST)
			subscription = new Subscription(query, query, "BillsSearchSubscriber", query);
		
		// no subscription offered for a bill code search
		// no subscription offered for "best match" searches
		
		if (subscription != null)
			Footer.setup(this, subscription, bills);
	}

	

//	public void onLoadBills(List<Bill> newBills) {
//		// if this is the first page of rolls, set up the subscription
//		if (bills.size() == 0) {
//			if (newBills.size() == 0) {
//				if (type == BILLS_SPONSOR)
//					Utils.showBack(this, R.string.empty_bills_sponsored);
//				else
//					Utils.showBack(this, R.string.empty_bills);
//				return;
//			} 
//			else if ((type == BILLS_CODE || type == BILLS_SEARCH) && newBills.size() == 1) {
//				startActivity(Utils.billIntent(this, newBills.get(0)));
//				finish();
//				return;
//			}
//		}
//		
//		// remove the placeholder and add the new bills in the array
//		if (bills.size() > 0) {
//			int lastIndex = bills.size() - 1;
//			if (bills.get(lastIndex) == null)
//				bills.remove(lastIndex);
//		}
//
//		bills.addAll(newBills);
//
//		// if we got back a full page of bills, there may be more yet to come
//		if (newBills.size() == PER_PAGE)
//			bills.add(null);
//
//		((BillAdapter) getListAdapter()).notifyDataSetChanged();
//		
//		setupSubscription();
//	}
//
//	public void onLoadBills(CongressException exception) {
//		if (bills.size() > 0) {
//			
//			lw.getLoading().setVisibility(View.GONE);
//			lw.getRetryContainer().setVisibility(View.VISIBLE);
//			
//			Button retry = lw.getRetry();
//			retry.setOnClickListener(new View.OnClickListener() {
//				public void onClick(View v) {
//					lw.getRetryContainer().setVisibility(View.GONE);
//					lw.getLoading().setVisibility(View.VISIBLE);
//					loadBills();
//				}
//			});
//
//		} else
//			Utils.showBack(this, R.string.error_connection);
//	}
	
	private static class LoadBillsTask extends AsyncTask<Void,Void,List<Bill>> {
		private BillListFragment context;
		private CongressException exception;

		public LoadBillsTask(BillListFragment context) {
			this.context = context;
			FragmentUtils.setupRTC(context);
		}

		@Override
		public List<Bill> doInBackground(Void... nothing) {
			try {
				int page = 1;
				
				Map<String,String> params = new HashMap<String,String>();
				
				switch (context.type) {
				case BILLS_RECENT:
					return BillService.recentlyIntroduced(page, PER_PAGE);
				case BILLS_LAW:
					return BillService.recentLaws(page, PER_PAGE);
				case BILLS_SPONSOR:
					return BillService.recentlySponsored(context.sponsor.id, page, PER_PAGE);
				case BILLS_CODE:
					params.put("code", context.code);
					return BillService.where(params, page, PER_PAGE);
				case BILLS_SEARCH_NEWEST:
					params.put("order", "introduced_at");
					return BillService.search(context.query, params, page, PER_PAGE);
				case BILLS_SEARCH_RELEVANT:
					params.put("order", "_score");
					
					// scope to current session only
					params.put("session", Bill.currentSession());
					
					return BillService.search(context.query, params, page, PER_PAGE);
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
			if (exception != null)
				context.onLoadBills(exception);
			else
				context.onLoadBills(bills);
		}
	}

	private static class BillAdapter extends ArrayAdapter<Bill> {
		private LayoutInflater inflater;
		private BillListFragment context;
		
		public BillAdapter(BillListFragment context, List<Bill> bills) {
			super(context.getActivity(), 0, bills);
			this.inflater = LayoutInflater.from(context.getActivity());
			this.context = context;
		}
		
		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Bill bill = getItem(position);
			
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
			
			String date = "";
			String action = "";
			switch (context.type) {
			case BILLS_LAW:
				date = shortDate(bill.enacted_at);
				action = "became law:";
				break;
			case BILLS_SEARCH_RELEVANT:
				date = longDate(bill.last_action_at);
				action = "was last active:";
				break;
			case BILLS_SEARCH_NEWEST:
			case BILLS_RECENT:
			case BILLS_SPONSOR:
			case BILLS_CODE:
			default:
				date = shortDate(bill.introduced_at);
				action = "was introduced:";
				break;
			}
			
			String code = Bill.formatCodeShort(bill.code);
			
			Spanned byline = Html.fromHtml("<b>" + code + "</b> " + action);
			holder.byline.setText(byline);
			holder.date.setText(date);

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
		
		private String shortDate(Date date) {
			SimpleDateFormat format = null;
			if (date.getYear() == new Date().getYear()) 
				format = new SimpleDateFormat("MMM d");
			else
				format = new SimpleDateFormat("MMM d, yyyy");
			return format.format(date);
		}
		
		private String longDate(Date date) {
			return new SimpleDateFormat("MMM d, yyyy").format(date);
		}
	}
	
}