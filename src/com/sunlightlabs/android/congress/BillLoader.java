package com.sunlightlabs.android.congress;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class BillLoader extends Activity implements LoadBillTask.LoadsBill {
	private LoadBillTask loadBillTask;
	private String bill_id;
	private Intent intent;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading_fullscreen);
		
		Intent i = getIntent();
		bill_id = i.getStringExtra("id");
		intent = (Intent) i.getParcelableExtra("intent");
		
		// if coming from a shortcut intent, there appears to be a bug with packaging sub-intents
		// and the intent will be null
		if (intent == null)
			intent = Utils.billPagerIntent();
		
		// if coming in from a link (in form /bill/:session/:formatted_code, e.g. "/bill/112/H.R. 2345")
		Uri uri = i.getData();
		if (uri != null) {
			List<String> segments = uri.getPathSegments();
			if (segments.size() == 3) { // coming in from floor
				String session = segments.get(1);
				String formattedCode = segments.get(2);
				String code = Bill.normalizeCode(formattedCode);
				bill_id = code + "-" + session;
			}
		}
		
		loadBillTask = (LoadBillTask) getLastNonConfigurationInstance();
		if (loadBillTask != null)
			loadBillTask.onScreenLoad(this);
		else
			loadBillTask = (LoadBillTask) new LoadBillTask(this, bill_id).execute("basic", "sponsor", "latest_upcoming", "last_version.urls");
		
		setupControls();
	}
	
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, Bill.formatCode(bill_id));
		Utils.setLoading(this, R.string.bill_loading);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadBillTask;
	}
	
	public void onLoadBill(Bill bill) {
		intent.putExtra("bill", bill);
		// pass entry info along, this loader class is an implementation detail
		startActivity(Analytics.passEntry(this, intent));
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