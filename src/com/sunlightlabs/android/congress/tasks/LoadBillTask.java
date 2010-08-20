package com.sunlightlabs.android.congress.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class LoadBillTask extends AsyncTask<String,Void,Bill> {
	private LoadsBill context;
	private CongressException exception;
	private String billId;
	
	public LoadBillTask(LoadsBill context, String billId) {
		this.context = context;
		this.billId = billId;
		Utils.setupDrumbone(context.getContext());
	}

	public void onScreenLoad(LoadsBill context) {
		this.context = context;
	}
	
	@Override
	public Bill doInBackground(String... sections) {
		try {
			return BillService.find(billId, sections[0]);
		} catch (CongressException exception) {
			this.exception = exception;
			return null;
		}
	}
	
	@Override
	public void onPostExecute(Bill bill) {
		if (isCancelled()) return;
		
		if (exception != null && bill == null)
			context.onLoadBill(exception);
		else
			context.onLoadBill(bill);
	}
	
	public interface LoadsBill {
		public void onLoadBill(Bill bill);
		public void onLoadBill(CongressException exception);
		public Context getContext();
	}
}