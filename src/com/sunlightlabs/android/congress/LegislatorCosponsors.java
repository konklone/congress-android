package com.sunlightlabs.android.congress;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.LegislatorListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;

public class LegislatorCosponsors extends FragmentActivity {
	
	String billId;
	Bill bill;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Analytics.track(this, "/bill/cosponsors");
		
		Intent intent = getIntent();
		billId = intent.getStringExtra("billId");
		bill = (Bill) intent.getSerializableExtra("bill");
		
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		findViewById(R.id.pager_titles).setVisibility(View.GONE);
		
		adapter.add("legislator_cosponsors", "Not seen", LegislatorListFragment.forBill(billId));
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, "Cosponsors for " + Bill.formatId(billId), Utils.billIntent(this, bill));
		ActionBarUtils.setTitleSize(this, 16);
	}
}