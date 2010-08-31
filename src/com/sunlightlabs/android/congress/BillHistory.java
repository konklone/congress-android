package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class BillHistory extends ListActivity implements LoadBillTask.LoadsBill {
	private LoadBillTask loadBillTask;
	private Bill bill;
	private String id;

	private String subscriptionName;
	private Footer footer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer);

		Bundle extras = getIntent().getExtras();
		Bill bill = (Bill) extras.getSerializable("bill");
		id = bill.id;
		subscriptionName = bill.code;
		
		BillHistoryHolder holder = (BillHistoryHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.loadBillTask = holder.loadBillTask;
			this.bill = holder.bill;
			if (loadBillTask != null)
				loadBillTask.onScreenLoad(this);
		}
		
		if (loadBillTask == null)
			loadBill();

		setupFooter();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		footer.onDestroy();
	}

	private void setupFooter() {
		footer = (Footer) findViewById(R.id.footer);
		footer.init(new Subscription(id, subscriptionName, "BillActionsFinder", id));
	}
	
	public void loadBill() {
		if (bill == null)
			loadBillTask = (LoadBillTask) new LoadBillTask(this, id).execute("actions");
		else
			displayBill();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new BillHistoryHolder(loadBillTask, bill);
	}

	public Context getContext() {
		return this;
	}
	
	public void onLoadBill(Bill bill) {
		this.loadBillTask = null;
		this.bill = bill;
		displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		Utils.showRefresh(this, R.string.error_connection);
	}
	
	public void displayBill() {
		if (bill.actions.size() > 0)
			setListAdapter(new BillActionAdapter(this, bill.actions));
		else
			Utils.showEmpty(this, R.string.bill_actions_empty);
	}
	
	protected class BillActionAdapter extends ArrayAdapter<Bill.Action> {
    	LayoutInflater inflater;
    	Resources resources;

        public BillActionAdapter(Activity context, ArrayList<Bill.Action> items) {
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
        public int getViewTypeCount() {
        	return 1;
        }

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null)
				view = inflater.inflate(R.layout.bill_action, null);
			
			Bill.Action action = getItem(position);
			
			String timestamp = new SimpleDateFormat("MMM dd, yyyy").format(action.acted_at);
			((TextView) view.findViewById(R.id.acted_at)).setText(timestamp);
			((TextView) view.findViewById(R.id.text)).setText(action.text);
			
			TextView typeView = (TextView) view.findViewById(R.id.type);
			String type = action.type;
			if (type.equals("vote") || type.equals("vote2") || type.equals("vote-aux")) {
				typeView.setText("Vote");
				typeView.setTextColor(resources.getColor(R.color.action_vote));
			} else if (type.equals("enacted")) {
				typeView.setText("Enacted");
				typeView.setTextColor(resources.getColor(R.color.action_enacted));
			} else if (type.equals("vetoed")) {
				typeView.setText("Vetoed");
				typeView.setTextColor(resources.getColor(R.color.action_vetoed));
			} else {
				typeView.setText("");
			}
			
			return view;
		}

    }
	
	static class BillHistoryHolder {
		LoadBillTask loadBillTask;
		Bill bill;
		
		public BillHistoryHolder(LoadBillTask loadBillTask, Bill bill) {
			this.loadBillTask = loadBillTask;
			this.bill = bill;
		}
	}
}