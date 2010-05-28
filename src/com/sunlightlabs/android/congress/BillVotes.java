package com.sunlightlabs.android.congress;

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
import android.widget.RelativeLayout;

import com.sunlightlabs.android.congress.BillHistory.BillHistoryHolder;
import com.sunlightlabs.android.congress.utils.LoadBillTask;
import com.sunlightlabs.android.congress.utils.LoadsBill;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;

public class BillVotes extends ListActivity implements LoadsBill {
	private LoadBillTask loadBillTask;
	private Bill bill;
	private String id;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);
		
		id = getIntent().getStringExtra("id");
		
		BillVotesHolder holder = (BillVotesHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.loadBillTask = holder.loadBillTask;
			this.bill = holder.bill;
			if (loadBillTask != null)
				loadBillTask.onScreenLoad(this);
		}
		
		if (loadBillTask == null)
			loadBill();
	}
	
	public void loadBill() {
		if (bill == null)
			loadBillTask = (LoadBillTask) new LoadBillTask(this, id).execute("votes");
		else
			displayBill();
	}
	
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
		Utils.alert(this, "Loaded " + bill.votes.size() + " votes");
		if (bill.votes.size() > 0)
			setListAdapter(new BillVoteAdapter(this, bill.votes));
		else
			Utils.showEmpty(this, R.string.bill_votes_empty);
	}
	
	protected class BillVoteAdapter extends ArrayAdapter<Bill.Vote> {
    	LayoutInflater inflater;
    	Resources resources;

        public BillVoteAdapter(Activity context, ArrayList<Bill.Vote> items) {
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

		public View getView(int position, View convertView, ViewGroup parent) {
			RelativeLayout view;
			if (convertView == null)
				view = (RelativeLayout) inflater.inflate(R.layout.bill_action, null);
			else
				view = (RelativeLayout) convertView;
			
			return view;
		}

    }
	
	static class BillVotesHolder {
		LoadBillTask loadBillTask;
		Bill bill;
		
		public BillVotesHolder(LoadBillTask loadBillTask, Bill bill) {
			this.loadBillTask = loadBillTask;
			this.bill = bill;
		}
	}
}