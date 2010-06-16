package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;

public class BillTabs extends TabActivity {
	private Bill bill;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bill);
		
		bill = (Bill) getIntent().getExtras().getSerializable("bill");
		
		setupControls();
		setupTabs();
	}
	
	public void setupControls() {
		((TextView) findViewById(R.id.title_text)).setText(Bill.formatCode(bill.code));
	}
	
	public void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		
		Utils.addTab(this, tabHost, "info_tab", "Details", detailsIntent(), res.getDrawable(R.drawable.tab_profile));
		Utils.addTab(this, tabHost, "history_tab", "History", historyIntent(), res.getDrawable(R.drawable.tab_news));
		
		if (bill.last_vote_at.getTime() > 0)
			Utils.addTab(this, tabHost, "voted_tab", "Votes", votesIntent(), res.getDrawable(R.drawable.tab_video));
		
		tabHost.setCurrentTab(0);
	}
	
	
	public Intent detailsIntent() {
		return Utils.billIntent(this, BillInfo.class, bill);
	}
	
	public Intent historyIntent() {
		return new Intent(this, BillHistory.class).putExtra("id", bill.id);
	}
	
	public Intent votesIntent() {
		return new Intent(this, BillVotes.class).putExtra("id", bill.id);
	}
}
