package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class BillLoader extends Activity implements LoadBillTask.LoadsBill {
	private LoadBillTask loadBillTask;
	private String id, code;
	private int tab;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading_fullscreen);
		
		Intent intent = getIntent();
		id = intent.getStringExtra("id");
		code = intent.getStringExtra("code");
		tab = intent.getIntExtra("tab", 0);
		
		loadBillTask = (LoadBillTask) getLastNonConfigurationInstance();
		if (loadBillTask != null)
			loadBillTask.onScreenLoad(this);
		else
			loadBillTask = (LoadBillTask) new LoadBillTask(this, id, tab).execute("basic,sponsor");
		
		setupControls();
	}
	
	
	public void setupControls() {
		if (code != null && !code.equals(""))
			Utils.setTitle(this, Bill.formatCode(code));
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadBillTask;
	}
	
	public void onLoadBill(Bill bill, int... tab) {
		startActivity(Utils.billIntent(this, bill));
		finish();
	}
	
	public void onLoadBill(CongressException exception) {
		if (exception instanceof CongressException.NotFound)
			Utils.alert(this, R.string.bill_not_found);
		else
			Utils.alert(this, R.string.error_connection);
		finish();
	}
	
	public Context getContext() {
		return this;
	}

}