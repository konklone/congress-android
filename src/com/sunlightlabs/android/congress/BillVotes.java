package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class BillVotes extends ListActivity implements LoadBillTask.LoadsBill {
	private LoadBillTask loadBillTask;
	private Bill bill;
	
	private Footer footer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer);

		Bundle extras = getIntent().getExtras();
		bill = (Bill) extras.getSerializable("bill");
		
		BillVotesHolder holder = (BillVotesHolder) getLastNonConfigurationInstance();
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
		footer.init(new Subscription(bill.id, Subscriber.notificationName(bill), "BillVotesSubscriber", bill.id));
	}

	public void loadBill() {
		if (bill.votes == null)
			loadBillTask = (LoadBillTask) new LoadBillTask(this, bill.id).execute("votes");
		else
			displayBill();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new BillVotesHolder(loadBillTask, bill);
	}
	
	public Context getContext() {
		return this;
	}
	
	public void onLoadBill(Bill bill) {
		this.loadBillTask = null;
		this.bill.votes = bill.votes;
		displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		Utils.showRefresh(this, R.string.error_connection);
	}
	
	public void displayBill() {
		if (bill.votes.size() > 0)
			setListAdapter(new BillVoteAdapter(this, bill.votes));
		else
			Utils.showEmpty(this, R.string.bill_votes_empty);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		String rollId = (String) v.getTag();
    	if (rollId != null)
    		startActivity(Utils.rollIntent(this, rollId));
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
        	return (getItem(position)).roll_id != null;
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
				view = inflater.inflate(R.layout.bill_vote, null);
			
			Bill.Vote vote = getItem(position);
			
			String timestamp = new SimpleDateFormat("MMM dd, yyyy").format(vote.voted_at);
			((TextView) view.findViewById(R.id.voted_at)).setText(timestamp);
			((TextView) view.findViewById(R.id.text)).setText(vote.text);
			((TextView) view.findViewById(R.id.chamber)).setText("the " + Utils.capitalize(vote.chamber));
			
			TextView resultView = (TextView) view.findViewById(R.id.result);
			String result = vote.result;
			if (result.equals("pass")) {
				resultView.setTextColor(resources.getColor(R.color.bill_passed));
				resultView.setText("Passed");
			} else if (result.equals("fail")) {
				resultView.setTextColor(resources.getColor(R.color.bill_failed));
				resultView.setText("Failed");
			}
			
			String roll_id = vote.roll_id;
			TextView typeMessage = (TextView) view.findViewById(R.id.type_message);
			if (roll_id != null) {
				typeMessage.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				typeMessage.setTextColor(resources.getColor(android.R.color.white));
				typeMessage.setText(R.string.bill_vote_roll);
				view.setTag(vote.roll_id);
			} else {
				typeMessage.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				typeMessage.setTextColor(resources.getColor(R.color.faded_grey));
				typeMessage.setText(R.string.bill_vote_not_roll);
				view.setTag(null);
			}
			
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