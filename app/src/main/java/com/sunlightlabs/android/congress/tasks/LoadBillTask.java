package com.sunlightlabs.android.congress.tasks;

import android.app.Fragment;
import android.os.AsyncTask;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class LoadBillTask extends AsyncTask<Void,Void,Bill> {
	private Fragment fragment;
	private CongressException exception;
	private String billId;

	public LoadBillTask(Fragment fragment, String billId) {
		this.fragment = fragment;
		this.billId = billId;
		Utils.setupAPI(fragment.getActivity());
	}

	@Override
	public Bill doInBackground(Void... nothing) {
		try {
			return BillService.find(billId);
		} catch (CongressException exception) {
			this.exception = exception;
			return null;
		}
	}

	@Override
	public void onPostExecute(Bill bill) {
		LoadsBill loader = (LoadsBill) fragment;
		if (exception != null && bill == null)
			loader.onLoadBill(exception);
		else
			loader.onLoadBill(bill);
	}

	public interface LoadsBill {
		void onLoadBill(Bill bill);
		void onLoadBill(CongressException exception);
	}
}