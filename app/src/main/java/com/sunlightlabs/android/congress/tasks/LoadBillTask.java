package com.sunlightlabs.android.congress.tasks;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class LoadBillTask extends AsyncTask<String,Void,Bill> {
	private Context context;
	private Fragment fragment;
	private CongressException exception;
	private String billId;
	
	public LoadBillTask(Context context, String billId) {
		this.context = context;
		this.billId = billId;
		Utils.setupAPI(context);
	}
	
	public LoadBillTask(Fragment fragment, String billId) {
		this.fragment = fragment;
		this.billId = billId;
		Utils.setupAPI(fragment.getActivity());
	}

	public void onScreenLoad(Context context) {
		this.context = context;
	}
	
	@Override
	public Bill doInBackground(String... sections) {
		try {
			return BillService.find(billId, sections);
		} catch (CongressException exception) {
			this.exception = exception;
			return null;
		}
	}
	
	@Override
	public void onPostExecute(Bill bill) {
		LoadsBill loader = (LoadsBill) (context != null ? context : fragment);
		if (exception != null && bill == null)
			loader.onLoadBill(exception);
		else
			loader.onLoadBill(bill);
	}
	
	public interface LoadsBill {
		public void onLoadBill(Bill bill);
		public void onLoadBill(CongressException exception);
	}
}