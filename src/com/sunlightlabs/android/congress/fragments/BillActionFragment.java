package com.sunlightlabs.android.congress.fragments;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class BillActionFragment extends ListFragment implements LoadBillTask.LoadsBill {
	private Bill bill;
	
	public static BillActionFragment create(Bill bill) {
		BillActionFragment frag = new BillActionFragment();
		Bundle args = new Bundle();
		
		args.putSerializable("bill", bill);
		
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public BillActionFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		bill = (Bill) args.getSerializable("bill");
		
		loadBill();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_footer, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		FragmentUtils.setLoading(this, R.string.bill_history_loading);
		
		if (bill.actions != null)
			displayBill();
	}

	private void setupSubscription() {
		Footer.setup(this, new Subscription(bill.id,  Subscriber.notificationName(bill), "ActionsBillSubscriber", bill.id), bill.actions);
	}
	
	public void loadBill() {
		new LoadBillTask(this, bill.id).execute("actions");
	}
	
	public void onLoadBill(Bill bill) {
		this.bill.actions = bill.actions;
		if (isAdded())
			displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.error_connection);
	}
	
	public void displayBill() {
		if (bill.actions != null && bill.actions.size() > 0)
			setListAdapter(new BillActionAdapter(this, bill.actions));
		else
			FragmentUtils.showEmpty(this, R.string.bill_actions_empty);
		
		setupSubscription();
	}
	
	static class BillActionAdapter extends ArrayAdapter<BillActionAdapter.ItemWrapper> {
    	LayoutInflater inflater;
    	Resources resources;
    	
    	private static final int TYPE_DATE = 0;
    	private static final int TYPE_ACTION = 1; 

        public BillActionAdapter(Fragment context, List<Bill.Action> actions) {
            super(context.getActivity(), 0, transformActions(actions));
            inflater = LayoutInflater.from(context.getActivity());
            resources = context.getResources();
        }
        
        @Override
        public boolean isEnabled(int position) {
        	return false;
        }
        
        @Override
        public boolean areAllItemsEnabled() {
        	return false;
        }
        
        @Override
        public int getItemViewType(int position) {
        	ItemWrapper item = getItem(position);
        	if (item instanceof BillActionAdapter.DateWrapper)
        		return BillActionAdapter.TYPE_DATE;
        	else
        		return BillActionAdapter.TYPE_ACTION;
        }
        
        @Override
        public int getViewTypeCount() {
        	return 2;
        }

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			ItemWrapper item = getItem(position);
			if (item instanceof DateWrapper)
				return dateView((DateWrapper) item);
			else 
				return contentView((ContentWrapper) item);
		}
		
		public View contentView(ContentWrapper wrapper) {
			View view = inflater.inflate(R.layout.bill_action, null);
			Bill.Action action = ((ContentWrapper) wrapper).content;
			
			String text = action.text;
			if (!text.endsWith("."))
				text += ".";
			
			String type = action.type;
			if (type.equals("vote") || type.equals("vote2") || type.equals("vote-aux") || type.equals("vetoed") || type.equals("enacted"))
				text = "<b>" + text + "</b>";
			
			((TextView) view).setText(Html.fromHtml(text));
			return view;
		}
		
		public View dateView(DateWrapper wrapper) {
			return Utils.dateView(getContext(), wrapper.date, Utils.shortDateThisYear(wrapper.date));
		}
		
		static class ItemWrapper {}
		
		static class DateWrapper extends ItemWrapper {
			Date date;
			
			public DateWrapper(Date date) {
				this.date = date;
			}
		}
		
		static class ContentWrapper extends ItemWrapper {
			Bill.Action content;
			
			public ContentWrapper(Bill.Action content) {
				this.content = content;
			}
		}
		
		static Date dateFor(Bill.Action action) {
			return action.acted_at;
		}
		
		static List<ItemWrapper> transformActions(List<Bill.Action> contents) {
			List<ItemWrapper> items = new ArrayList<ItemWrapper>();
			
			int currentMonth = -1;
			int currentDay = -1;
			int currentYear = -1;
			
			for (int i=0; i<contents.size(); i++) {
				Bill.Action content = contents.get(i);
				
				Date contentDate = dateFor(content);
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.setTime(contentDate);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				int year = calendar.get(Calendar.YEAR);
				
				if (currentMonth != month || currentDay != day || currentYear != year) {
					
					items.add(new DateWrapper(contentDate));
					
					currentMonth = month;
					currentDay = day;
					currentYear = year;
				}
				
				items.add(new ContentWrapper(content));
			}
			
			return items;
		}

    }
}