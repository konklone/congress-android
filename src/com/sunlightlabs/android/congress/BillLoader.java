package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.sunlightlabs.android.congress.utils.LoadBillTask;
import com.sunlightlabs.android.congress.utils.LoadsBill;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;

public class BillLoader extends Activity implements LoadsBill {
	private LoadBillTask loadBillTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading_fullscreen);
		
		String id = getIntent().getStringExtra("id");
		
		loadBillTask = (LoadBillTask) getLastNonConfigurationInstance();
		if (loadBillTask != null)
			loadBillTask.onScreenLoad(this);
		else
			loadBillTask = (LoadBillTask) new LoadBillTask(this, id).execute("basic,sponsor");
	}
	
	public Object onRetainNonConfigurationInstance() {
		return loadBillTask;
	}
	
	public void onLoadBill(Bill bill) {
		startActivity(Utils.billIntent(this, bill));
		finish();
	}
	
	public void onLoadBill(CongressException exception) {
		Utils.alert(this, R.string.error_connection);
		finish();
	}
	
	public Context getContext() {
		return this;
	}

}