package com.sunlightlabs.android.congress.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.sunlightlabs.android.congress.BillPager;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class BillLoaderFragment extends Fragment implements LoadBillTask.LoadsBill {
	private static String FRAGMENT_TAG = "BillLoaderFragment";
	
	public BillPager context;
	public Bill bill;
	public CongressException exception;
	
	public static void start(BillPager context) {
		start(context, false);
	}
	
	public static void start(BillPager context, boolean restart) {
		FragmentManager manager = context.getSupportFragmentManager();
		BillLoaderFragment fragment = (BillLoaderFragment) manager.findFragmentByTag(FRAGMENT_TAG);
		if (fragment == null) {
			fragment = new BillLoaderFragment();
			fragment.context = context;
			fragment.setRetainInstance(true);
			manager.beginTransaction().add(fragment, FRAGMENT_TAG).commit();
		} else if (restart) {
			fragment.context = context;
			fragment.run();
		} else
			fragment.context = context; // still assign context
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		run();
	}
	
	public void run() {
		new LoadBillTask(this, context.bill_id).execute(BillService.basicFields);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (this.bill != null)
			context.onLoadBill(bill);
		else if (this.exception != null)
			context.onLoadBill(this.exception);
	}
	
	public BillLoaderFragment() {}
	
	// pass through
	public void onLoadBill(Bill bill) {
		this.bill = bill;
		context.onLoadBill(bill);
	}
	
	public void onLoadBill(CongressException exception) {
		this.exception = exception;
		context.onLoadBill(exception);
	}
}