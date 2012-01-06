package com.sunlightlabs.android.congress.fragments;

import java.util.Date;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.utils.DateAdapterHelper;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class BillActionFragment extends ListFragment implements LoadBillTask.LoadsBill, DateAdapterHelper.StickyHeader {
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
			setListAdapter(new BillActionAdapter(this).adapterFor(bill.actions));
		else
			FragmentUtils.showEmpty(this, R.string.bill_actions_empty);
		
		setupSubscription();
	}
	
	static class BillActionAdapter extends DateAdapterHelper<Bill.Action> {
		public BillActionAdapter(ListFragment context) {
			super(context);
		}

		@Override
		public Date dateFor(Bill.Action action) {
			return action.acted_at;
		}
		
		@Override
		public View contentView(Bill.Action action) {
			View view = inflater.inflate(R.layout.bill_action, null);
			
			String text = action.text;
			if (!text.endsWith("."))
				text += ".";
			
			String type = action.type;
			if (type.equals("vote") || type.equals("vote2") || type.equals("vote-aux") || type.equals("vetoed") || type.equals("enacted"))
				text = "<b>" + text + "</b>";
			
			((TextView) view).setText(Html.fromHtml(text));
			return view;
		}
		
		@Override
        public void updateStickyHeader(Date date, View view, TextView left, TextView right) {
        	String nearbyDate = Utils.nearbyDate(date);
    		String fullDate = Utils.fullDateThisYear(date);
    		
    		if (nearbyDate != null) {
    			left.setText(nearbyDate);
    			right.setText(fullDate);
    		} else {
    			left.setText(fullDate);
    			right.setText("");
    		}
        }
	}
	
	
}