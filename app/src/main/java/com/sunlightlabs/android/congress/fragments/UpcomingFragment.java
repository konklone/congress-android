package com.sunlightlabs.android.congress.fragments;

import android.app.Fragment;
import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.UpcomingBill;
import com.sunlightlabs.congress.services.UpcomingBillService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UpcomingFragment extends ListFragment {

	List<UpcomingBill> upcomingBills;

	public static UpcomingFragment newInstance() {
		UpcomingFragment fragment = new UpcomingFragment();
		fragment.setRetainInstance(true);
		return fragment;
	}

	public UpcomingFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		loadUpcoming();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_no_divider, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setupControls();

		if (upcomingBills != null)
			displayUpcomingBills();
	}

	private void setupControls() {
		FragmentUtils.setLoading(this, R.string.upcoming_loading);

		getView().findViewById(R.id.refresh).setOnClickListener(v -> {
			upcomingBills = null;
			FragmentUtils.showLoading(UpcomingFragment.this);
			loadUpcoming();
		});
	}

	public void loadUpcoming() {
		new UpcomingBillsTask(this).execute();
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		if (isAdded()) {
			String bill_id = ((UpcomingAdapter.Bill) parent.getItemAtPosition(position)).id;
			Analytics.billUpcoming(getActivity(), bill_id);
			startActivity(Utils.billIntent(bill_id));
		}
	}

	private void onLoadUpcomingBills(List<UpcomingBill> upcomingBills) {
		this.upcomingBills = upcomingBills;

		if (isAdded())
			displayUpcomingBills();
	}

	private void onLoadUpcomingBills(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.upcoming_error);
	}

	private void displayUpcomingBills() {
		if (upcomingBills.size() > 0)
			setListAdapter(new UpcomingAdapter(this, UpcomingAdapter.wrapUpcoming(upcomingBills)));
		else
			FragmentUtils.showEmpty(this, R.string.upcoming_empty);
	}

	static class UpcomingAdapter extends ArrayAdapter<UpcomingAdapter.Item> {
		LayoutInflater inflater;

		private static final int TYPE_DATE = 0;
		private static final int TYPE_BILL = 1;

		public UpcomingAdapter(Fragment context, List<UpcomingAdapter.Item> items) {
			super(context.getActivity(), 0, items);
			this.inflater = LayoutInflater.from(context.getActivity());
		}

		@Override
        public boolean isEnabled(int position) {
        	return getItemViewType(position) == UpcomingAdapter.TYPE_BILL;
        }

		@Override
        public boolean areAllItemsEnabled() {
        	return false;
        }

		@Override
        public int getItemViewType(int position) {
        	Item item = getItem(position);
        	if (item instanceof UpcomingAdapter.Date)
        		return UpcomingAdapter.TYPE_DATE;
        	else
        		return UpcomingAdapter.TYPE_BILL;
        }

		@Override
        public int getViewTypeCount() {
        	return 2;
        }

		static List<UpcomingAdapter.Item> wrapUpcoming(List<UpcomingBill> upcomingBills) {
			List<Item> items = new ArrayList<>();

			SimpleDateFormat dateNameFormat = new SimpleDateFormat("EEEE");
			SimpleDateFormat dateFullFormat = new SimpleDateFormat("MMM d");
			SimpleDateFormat testFormat = new SimpleDateFormat("yyyy-MM-dd");

			String today = testFormat.format(Calendar.getInstance().getTime());
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, 1);
			String tomorrow = testFormat.format(cal.getTime());

			String currentDayRange = "";

			for (int i=0; i<upcomingBills.size(); i++) {
				UpcomingBill upcomingBill = upcomingBills.get(i);

				String range = (upcomingBill.range == null) ? "none" : upcomingBill.range;
				String testDay = (upcomingBill.legislativeDay == null) ? "future" : testFormat.format(
						upcomingBill.legislativeDay);
				String testDayRange = testDay + "-" + range;

				// a new header is needed
				if (!currentDayRange.equals(testDayRange)) {
					Date date = new Date();

					// if no legislative_day, just call it the future
					if (testDay.equals("future")) {
						date.dateName = "SOMETIME";
					} else {

						// specific day
						switch (range) {
							case "day":
								if (today.equals(testDay))
									date.dateName = "TODAY";
								else if (tomorrow.equals(testDay))
									date.dateName = "TOMORROW";
								else
									date.dateName = dateNameFormat.format(upcomingBill.legislativeDay).toUpperCase();

								date.dateFull = dateFullFormat.format(upcomingBill.legislativeDay).toUpperCase();
								break;
							// week of this day
							case "week":
								date.dateName = "WEEK OF";
								date.dateFull = dateFullFormat.format(upcomingBill.legislativeDay).toUpperCase();
								break;
							// indefinite range, null or any other value (future-proof)
							default:
								// we're only here if range is indefinite, but legislative_day has a value
								// still, make no promises, this is an unexpected case
								date.dateName = "SOMETIME";
								break;
						}
					}

					items.add(date);

					currentDayRange = testDayRange;
				}

				// in case of "H.R. ____" or other changes, should safely skip
				if (upcomingBill.billId == null || upcomingBill.description == null)
					continue;

				String[] pieces = com.sunlightlabs.congress.models.Bill.splitBillId(upcomingBill.billId);

				Bill bill = new Bill();
				bill.bill_type = pieces[0];
				bill.number = Integer.valueOf(pieces[1]);
				bill.title = Utils.truncate(upcomingBill.description, 55);
				bill.id = upcomingBill.billId;

				items.add(bill);
			}

			return items;
		}

		static class Item {
		}

		static class Date extends Item {
			String dateName, dateFull;
		}

		static class Bill extends Item {
			String id, bill_type, title;
			int number;
			String context;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Item item = getItem(position);
			if (item instanceof Date) {
				if (view == null)
					view = inflater.inflate(R.layout.upcoming_date, null);

				Date date = (Date) item;

				((TextView) view.findViewById(R.id.date_left)).setText(date.dateName);
				((TextView) view.findViewById(R.id.date_right)).setText(date.dateFull);
			} else { // instanceof Action
				if (view == null)
					view = inflater.inflate(R.layout.upcoming_bill, null);

				Bill bill = (Bill) item;

				((TextView) view.findViewById(R.id.code)).setText(com.sunlightlabs.congress.models.Bill.formatCode(
						bill.bill_type, bill.number));
				((TextView) view.findViewById(R.id.title)).setText(bill.title);
			}

			return view;
		}
	}

	static class UpcomingBillsTask extends AsyncTask<String, Void, List<UpcomingBill>> {
		private UpcomingFragment context;
		private CongressException exception;

		public UpcomingBillsTask(UpcomingFragment context) {
			FragmentUtils.setupAPI(context);
			this.context = context;
		}

		@Override
		protected List<UpcomingBill> doInBackground(String... params) {
			try {
				return UpcomingBillService.comingUp();
			} catch (CongressException e) {
				this.exception = new CongressException(e, "Error loading upcoming activity.");
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<UpcomingBill> result) {
			if (result != null && exception == null)
				context.onLoadUpcomingBills(result);
			else
				context.onLoadUpcomingBills(exception);
		}
	}
}