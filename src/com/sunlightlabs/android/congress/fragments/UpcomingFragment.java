package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.UpcomingBill;
import com.sunlightlabs.congress.services.UpcomingBillService;

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
		
		new UpcomingBillsTask(this).execute();
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
		
		// TODO: setup refresh button behavior
	}
	
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		if (isAdded()) {
			Bill bill = ((UpcomingAdapter.Bill) parent.getItemAtPosition(position)).bill;
			startActivity(Utils.billLoadIntent(bill.id, bill.code));
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
	
	static class UpcomingBillsTask extends AsyncTask<String, Void, List<UpcomingBill>> {
		private UpcomingFragment context;
		private CongressException exception;

		public UpcomingBillsTask(UpcomingFragment context) {
			FragmentUtils.setupRTC(context);
			this.context = context;
		}

		@Override
		protected List<UpcomingBill> doInBackground(String... params) {
			try {
				Date today = new GregorianCalendar().getTime();
				return UpcomingBillService.comingUp(today);
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

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Item item = getItem(position);
			if (item instanceof Date) {
				if (view == null)
					view = inflater.inflate(R.layout.upcoming_date, null);
				
				Date date = (Date) item;
				
				((TextView) view.findViewById(R.id.date_name)).setText(date.dateName);
				((TextView) view.findViewById(R.id.date_full)).setText(date.dateFull);
			} else { // instanceof Action
				if (view == null)
					view = inflater.inflate(R.layout.upcoming_bill, null);
				
				Bill bill = (Bill) item;
				
				//((TextView) view.findViewById(R.id.code)).setText(bill.code);
				((TextView) view.findViewById(R.id.title)).setText(bill.title);
			}

			return view;
		}
		
		static class Item {}
		static class Date extends Item {
			String dateName, dateFull;
		}
		static class Bill extends Item {
			String code, title;
			com.sunlightlabs.congress.models.Bill bill;
			String context;
		}
		
		static List<UpcomingAdapter.Item> wrapUpcoming(List<UpcomingBill> upcomingBills) {
			List<Item> items = new ArrayList<Item>();
			
			SimpleDateFormat dateNameFormat = new SimpleDateFormat("EEEE");
			SimpleDateFormat dateFullFormat = new SimpleDateFormat("MMM d");
			SimpleDateFormat testFormat = new SimpleDateFormat("yyyy-MM-dd");
			
			String today = testFormat.format(Calendar.getInstance().getTime());
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, 1);
			String tomorrow = testFormat.format(cal.getTime());
			
			
			String currentDay = "";
			
			for (int i=0; i<upcomingBills.size(); i++) {
				UpcomingBill upcomingBill = upcomingBills.get(i);
				
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.setTime(upcomingBill.legislativeDay);
				
				String testDay = testFormat.format(upcomingBill.legislativeDay);
				
				if (!currentDay.equals(testDay)) {
					Date date = new Date();
					if (today.equals(testDay))
						date.dateName = "TODAY";
					else if (tomorrow.equals(testDay))
						date.dateName = "TOMORROW";
					else
						date.dateName = dateNameFormat.format(upcomingBill.legislativeDay).toUpperCase();
					
					date.dateFull = dateFullFormat.format(upcomingBill.legislativeDay).toUpperCase();
					items.add(date);
					
					currentDay = testDay;
				}
				
				com.sunlightlabs.congress.models.Bill rootBill = upcomingBill.bill;
				if (rootBill == null)
					continue;
				
				Bill bill = new Bill();
				
				bill.code = com.sunlightlabs.congress.models.Bill.formatCodeShort(rootBill.code);
				
				String title;
				if (rootBill.short_title != null && !rootBill.short_title.equals(""))
					title = rootBill.short_title;
				else
					title = Utils.truncate(rootBill.official_title, 60);
				
				bill.title = title;
				bill.bill = rootBill;
				
				items.add(bill);
			}
			
			return items;
		}
	}
}