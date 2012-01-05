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
import com.sunlightlabs.android.congress.notifications.PaginationAdapter;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.BillService;

public class BillListFragment extends ListFragment implements PaginationAdapter.Paginates {
	
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
	
	PaginationAdapter pager;
	View loadingView;
	
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
		
		loadingView = LayoutInflater.from(getActivity()).inflate(R.layout.loading, null);
		loadingView.setVisibility(View.GONE);
		pager = new PaginationAdapter(this);

		FragmentUtils.setLoading(this, R.string.bills_loading);
	}
	
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		startActivity(Utils.billIntent(getActivity(), (Bill) parent.getItemAtPosition(position)));
	}

	private void refresh() {
		bills = null;
		FragmentUtils.setLoading(this, R.string.bills_loading);
		FragmentUtils.showLoading(this);
		loadBills();
	}

	public void loadBills() {
		new LoadBillsTask(this, 1).execute();
	}
	
	public void loadNextPage(int page) {
		getListView().setOnScrollListener(null);
		loadingView.setVisibility(View.VISIBLE);
		new LoadBillsTask(this, page).execute();
	}
	
	// handles coming in with any page of bills, even the first one
	public void onLoadBills(List<Bill> bills, int page) {
		if (page == 1) {
			this.bills = bills;
			if (isAdded())
				displayBills();
		} else {
			this.bills.addAll(bills);
			if (isAdded()) {
				loadingView.setVisibility(View.GONE);
				((BillAdapter) getListAdapter()).notifyDataSetChanged();
			}
		}
		
		// only re-enable the pagination if we got a full page back
		if (bills.size() == PER_PAGE)
			getListView().setOnScrollListener(pager);
	}
	
	public void onLoadBills(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.bills_error);
	}
	
	// only run for the first page of bill results
	public void displayBills() {
		if (bills.size() > 0) {
			getListView().addFooterView(loadingView);
			getListView().setOnScrollListener(pager);
			setListAdapter(new BillAdapter(this, bills));
			setupSubscription();
		} else {
			if (type == BILLS_SEARCH_NEWEST) {
				FragmentUtils.showEmpty(this, R.string.bills_empty_search_newest);
				setupSubscription();
			} else if (type == BILLS_SEARCH_RELEVANT) {
				FragmentUtils.showEmpty(this, R.string.bills_empty_search_relevant);
				setupSubscription();
			} else if (type == BILLS_CODE) {
				FragmentUtils.showEmpty(this, R.string.bills_empty_code);
				setupSubscription();
			} else if (type == BILLS_SPONSOR) {
				FragmentUtils.showEmpty(this, R.string.bills_empty_sponsor);
				setupSubscription();
			} else // recent bills, recent laws
				FragmentUtils.showRefresh(this, R.string.bills_error); // should not happen
		}
	}
	
	private void setupSubscription() {
		Subscription subscription = null;
		if (type == BILLS_RECENT)
			subscription = new Subscription("RecentBills", getResources().getString(R.string.subscriber_bills_new), "BillsRecentSubscriber", null);
		else if (type == BILLS_SPONSOR)
			subscription = new Subscription(sponsor.id, Subscriber.notificationName(sponsor), "BillsLegislatorSubscriber", null);
		else if (type == BILLS_LAW)
			subscription = new Subscription("RecentLaws", getResources().getString(R.string.subscriber_bills_law), "BillsLawsSubscriber", null);
		else if (type == BILLS_SEARCH_NEWEST)
			subscription = new Subscription(query, query, "BillsSearchSubscriber", query);
		
		// no subscription offered for a bill code search
		// no subscription offered for "best match" searches
		
		if (subscription != null)
			Footer.setup(this, subscription, bills);
	}

	
	private static class LoadBillsTask extends AsyncTask<Void,Void,List<Bill>> {
		private BillListFragment context;
		private CongressException exception;
		private int page;

		public LoadBillsTask(BillListFragment context, int page) {
			this.context = context;
			this.page = page;
			FragmentUtils.setupRTC(context);
		}

		@Override
		public List<Bill> doInBackground(Void... nothing) {
			try {
				
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
				context.onLoadBills(bills, page);
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