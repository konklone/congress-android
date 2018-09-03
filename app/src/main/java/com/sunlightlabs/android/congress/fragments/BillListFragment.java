package com.sunlightlabs.android.congress.fragments;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.PaginationListener;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.BillService;
import com.sunlightlabs.congress.services.ProPublica;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BillListFragment extends ListFragment implements PaginationListener.Paginates {

	public static final int BILLS_ACTIVE = 0;
	public static final int BILLS_ALL = 1;
	public static final int BILLS_SPONSOR = 2;
	public static final int BILLS_SEARCH_NEWEST = 3;
	public static final int BILLS_SEARCH_RELEVANT = 4;
	public static final int BILLS_CODE = 5;
	public static final int BILLS_LAW = 6;

	List<Bill> bills;
	List<String> newIds;

	int type;
	Legislator sponsor;
	String bill_type;
	int number;
	String query;

	PaginationListener pager;
	View loadingView;

	public static Fragment forAll() {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", BILLS_ALL);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}

	public static Fragment forActive() {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", BILLS_ACTIVE);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}

	public static Fragment forLaw() {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", BILLS_LAW);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}

	public static Fragment forSponsor(Legislator sponsor) {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", BILLS_SPONSOR);
		args.putSerializable("sponsor", sponsor);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}

	public static Fragment forCode(String bill_type, int number) {
		BillListFragment frag = new BillListFragment();
		Bundle args = new Bundle();
		args.putInt("type", BILLS_CODE);
		args.putString("bill_type", bill_type);
		args.putInt("number", number);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}

	public static Fragment forSearch(String query, int type) {
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
		bill_type = args.getString("bill_type");
		number = args.getInt("number");
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
		newIds = FragmentUtils.newIds(this, subscriberClass());
		setupControls();
		if (bills != null)
			displayBills();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (bills != null)
			setupSubscription();
	}

	public void setupControls() {
		getView().findViewById(R.id.refresh).setOnClickListener(v -> refresh());

		if (type != BILLS_CODE) {
			loadingView = LayoutInflater.from(getActivity()).inflate(R.layout.loading_page, null);
			loadingView.setVisibility(View.GONE);
			getListView().addFooterView(loadingView);
			
			pager = new PaginationListener(this);
			getListView().setOnScrollListener(pager);
		}
		FragmentUtils.setLoading(this, R.string.bills_loading);
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		Bill bill = (Bill) parent.getItemAtPosition(position);
		startActivity(Utils.billIntent(bill.id));
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

	@Override
	public void loadNextPage(int page) {
		getListView().setOnScrollListener(null);
		loadingView.setVisibility(View.VISIBLE);
		new LoadBillsTask(this, page).execute();
	}

	// handles coming in with any page of bills, even the first one
	public void onLoadBills(List<Bill> bills, int page) {
		if (!isAdded())
			return;

		if (page == 1) {
			// if new IDs, sort them to the top.
			// (new IDs will only affect page 1)
			if (newIds != null) {
				Collections.sort(bills, (a, b) -> {
					boolean hasA = newIds.contains(a.id);
					boolean hasB = newIds.contains(b.id);
					if (hasA && !hasB)
						return -1;
					else if (!hasA && hasB)
						return 1;
					else
						return 0;
				});
			}

			this.bills = bills;
			displayBills();
		} else {
			this.bills.addAll(bills);
			loadingView.setVisibility(View.GONE);
			((BillAdapter) getListAdapter()).notifyDataSetChanged();
		}

		// only re-enable the pagination if we got a full page back
		if (bills.size() >= ProPublica.PER_PAGE)
			getListView().setOnScrollListener(pager);
	}

	public void onLoadBills(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.bills_error);
	}

	// only run for the first page of bill results
	public void displayBills() {
		if (bills.size() > 0) {
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
			} else // active bills, all bills
				FragmentUtils.showRefresh(this, R.string.bills_error); // should not happen
		}
	}

	private String subscriberClass() {
		if (type == BILLS_ALL)
			return "BillsRecentSubscriber";
		else if (type == BILLS_ACTIVE)
			return "BillsActiveSubscriber";
		else if (type == BILLS_SEARCH_NEWEST)
			return "BillsSearchSubscriber";
		else
			return null;
	}

	private void setupSubscription() {
		Subscription subscription = null;
		if (type == BILLS_ALL)
			subscription = new Subscription("RecentBills", getResources().getString(R.string.subscriber_bills_all), "BillsRecentSubscriber", null);
		else if (type == BILLS_SPONSOR)
			subscription = new Subscription(sponsor.bioguide_id, Subscriber.notificationName(sponsor), "BillsLegislatorSubscriber", null);
		else if (type == BILLS_ACTIVE)
			subscription = new Subscription("ActiveBills", getResources().getString(R.string.subscriber_bills_active), "BillsActiveSubscriber", null);
		else if (type == BILLS_LAW)
			subscription = new Subscription("EnactedLegislation", getResources().getString(R.string.subscriber_bills_law), "BillsLawSubscriber", null);
		else if (type == BILLS_SEARCH_NEWEST)
			subscription = new Subscription(query, query, "BillsSearchSubscriber", query);

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
			FragmentUtils.setupAPI(context);
		}

		@Override
		public List<Bill> doInBackground(Void... nothing) {
			try {
				switch (context.type) {
				case BILLS_ALL:
					return BillService.recentlyIntroduced(page);
				case BILLS_ACTIVE:
					return BillService.recentlyActive(page);
				case BILLS_LAW:
					return BillService.recentlyLaw(page);
				case BILLS_SPONSOR:
					return BillService.recentlySponsored(context.sponsor.bioguide_id, page);
				case BILLS_CODE:
				    int congress = Bill.currentCongress();
				    String bill_id = context.bill_type + String.valueOf(context.number) + "-" + String.valueOf(congress);
					List<Bill> matches = new ArrayList<>();
                    Bill bill = BillService.find(bill_id);
                    if (bill != null) matches.add(bill);
                    return matches;
				case BILLS_SEARCH_NEWEST:
					return BillService.searchLatest(context.query, page);
				case BILLS_SEARCH_RELEVANT:
					return BillService.searchRelevant(context.query, page);
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
				holder.code = view.findViewById(R.id.code);
				holder.date = view.findViewById(R.id.date);
				holder.title = view.findViewById(R.id.title);
				holder.last_action = view.findViewById(R.id.last_action);
				holder.newResult = view.findViewById(R.id.new_result);

				view.setTag(holder);
			} else
				holder = (ViewHolder) view.getTag();

			switch (context.type) {
			case BILLS_ACTIVE:
				shortDate(holder.date, bill.last_action_on);
				break;
			case BILLS_LAW:
				shortDate(holder.date, bill.enacted_on);
				break;
			case BILLS_SEARCH_RELEVANT:
                longDate(holder.date, bill.last_action_on);
                break;
			case BILLS_SEARCH_NEWEST:
				shortDate(holder.date, bill.last_action_on);
				break;
			case BILLS_ALL:
			case BILLS_SPONSOR:
			case BILLS_CODE:
			default:
				shortDate(holder.date, bill.introduced_on);
				break;
			}

			holder.code.setText(Bill.formatCode(bill.bill_type, bill.number));

            holder.title.setTextSize(14);
			if (bill.short_title != null)
				holder.title.setText(Utils.truncate(bill.short_title, 250));
			else
				holder.title.setText(R.string.bill_no_title);

			if (context.type == BILLS_ACTIVE) {
				holder.last_action.setText(actionText(bill.lastAction));
				holder.last_action.setVisibility(View.VISIBLE);
			} else
				holder.last_action.setVisibility(View.GONE);

			if (context.newIds != null && context.newIds.contains(bill.id))
				holder.newResult.setVisibility(View.VISIBLE);
			else
				holder.newResult.setVisibility(View.GONE);

			return view;
		}

		static class ViewHolder {
			TextView code, date, title, last_action;
			View newResult;
		}

		private void shortDate(TextView view, Date date) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			int dateYear = calendar.get(Calendar.YEAR);
			int todayYear = Calendar.getInstance().get(Calendar.YEAR);
			if (dateYear == todayYear) {
				view.setTextSize(18);
				view.setText(new SimpleDateFormat("MMM d", Locale.US).format(date).toUpperCase(Locale.US));
			} else
				longDate(view, date);
		}

		private void longDate(TextView view, Date date) {
			view.setTextSize(14);
			view.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(date).toUpperCase(Locale.US));
		}

		private String actionText(Bill.Action action) {
            if (action.description != null)
			    return Utils.truncate(action.description, 80);
            else
                return "";
		}
	}
}