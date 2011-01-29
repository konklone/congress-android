package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class BillHistory extends ListActivity implements LoadBillTask.LoadsBill {
	private LoadBillTask loadBillTask;
	private Bill bill;
	private Footer footer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer);

		Bundle extras = getIntent().getExtras();
		bill = (Bill) extras.getSerializable("bill");
		
		BillHistoryHolder holder = (BillHistoryHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.loadBillTask = holder.loadBillTask;
			this.bill = holder.bill;
			this.footer = holder.footer;
		}
		
		if (footer != null)
			footer.onScreenLoad(this);
		else
			footer = Footer.from(this);
		
		if (loadBillTask != null)
			loadBillTask.onScreenLoad(this);
		else
			loadBill();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new BillHistoryHolder(loadBillTask, bill, footer);
	}

	public Context getContext() {
		return this;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (bill.actions != null)
			setupSubscription();
	}

	private void setupSubscription() {
		footer.init(new Subscription(bill.id,  Subscriber.notificationName(bill), "ActionsBillSubscriber", bill.id), bill.actions);
	}
	
	public void loadBill() {
		if (bill.actions == null)
			loadBillTask = (LoadBillTask) new LoadBillTask(this, bill.id).execute("actions");
		else
			displayBill();
	}
	
	public void onLoadBill(Bill bill) {
		this.loadBillTask = null;
		this.bill.actions = bill.actions;
		displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		Utils.showRefresh(this, R.string.error_connection);
	}
	
	public void displayBill() {
		if (bill.actions != null && bill.actions.size() > 0)
			setListAdapter(new BillActionAdapter(this, BillActionAdapter.transformActions(bill.actions)));
		else
			Utils.showEmpty(this, R.string.bill_actions_empty);
		
		setupSubscription();
	}
	
	static class BillActionAdapter extends ArrayAdapter<BillActionAdapter.Item> {
    	LayoutInflater inflater;
    	Resources resources;
    	
    	public static final int TYPE_DATE = 0;
    	public static final int TYPE_ACTION = 1; 

        public BillActionAdapter(Activity context, List<BillActionAdapter.Item> items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
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
        	Item item = getItem(position);
        	if (item instanceof BillActionAdapter.Date)
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
			Item item = getItem(position);
			if (item instanceof Date) {
				if (view == null)
					view = inflater.inflate(R.layout.bill_action_date, null);
				
				((TextView) view.findViewById(R.id.day)).setText(((Date) item).timestamp);				
				
			} else { // instanceof Action
				if (view == null)
					view = inflater.inflate(R.layout.bill_action, null);
				
				((TextView) view).setText(Html.fromHtml(((Action) item).text));
			}

			return view;
		}
		
		static class Item {}
		
		static class Date extends Item {
			String timestamp;
			
			public Date(String timestamp) {
				this.timestamp = timestamp;
			}
		}
		
		static class Action extends Item {
			String text;
			
			public Action(String text) {
				this.text = text;
			}
		}
		
		static List<Item> transformActions(List<Bill.Action> actions) {
			List<Item> items = new ArrayList<Item>();
			
			SimpleDateFormat otherYearFormat = new SimpleDateFormat("MMM dd, yyyy");
			SimpleDateFormat thisYearFormat = new SimpleDateFormat("MMM dd");
			
			int thisYear = new GregorianCalendar().get(Calendar.YEAR); 
			
			int currentMonth = -1;
			int currentDay = -1;
			int currentYear = -1;
			
			for (int i=0; i<actions.size(); i++) {
				Bill.Action action = actions.get(i);
				
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.setTime(action.acted_at);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				int year = calendar.get(Calendar.YEAR);
				
				if (currentMonth != month || currentDay != day || currentYear != year) {
					String timestamp = ((year == thisYear) ? thisYearFormat : otherYearFormat).format(action.acted_at);
					items.add(new Date(timestamp));
					
					currentMonth = month;
					currentDay = day;
					currentYear = year;
				}
				
				String text = action.text;
				if (!text.endsWith("."))
					text += ".";
				
				String type = action.type;
				if (type.equals("vote") || type.equals("vote2") || type.equals("vote-aux") || type.equals("vetoed") || type.equals("enacted"))
					text = "<b>" + text + "</b>";
				items.add(new Action(text));
			}
			
			return items;
		}

    }
	
	static class BillHistoryHolder {
		LoadBillTask loadBillTask;
		Bill bill;
		Footer footer;
		
		public BillHistoryHolder(LoadBillTask loadBillTask, Bill bill, Footer footer) {
			this.loadBillTask = loadBillTask;
			this.bill = bill;
			this.footer = footer;
		}
	}
}