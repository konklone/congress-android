package com.sunlightlabs.android.congress;

import com.sunlightlabs.congress.java.Bill;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;

public class BillTabs extends TabActivity {
	private String id, type, code, short_title, official_title;
	private int number, session;
	private boolean passed, vetoed, awaiting_signature, enacted;
	private String house_result, senate_result, override_house_result, override_senate_result;
	private long introduced_at, house_result_at, senate_result_at, passed_at;
	private long vetoed_at, override_house_result_at, override_senate_result_at;
	private long awaiting_signature_since, enacted_at;
	
	private String sponsor_id, sponsor_party, sponsor_state, sponsor_title;
	private String sponsor_first_name, sponsor_last_name, sponsor_nickname;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bill);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		type = extras.getString("type");
		number = extras.getInt("number");
		session = extras.getInt("session");
		code = extras.getString("code");
		short_title = extras.getString("short_title");
		official_title = extras.getString("official_title");
		
		introduced_at = extras.getLong("introduced_at", 0);
		house_result = extras.getString("house_result");
		house_result_at = extras.getLong("house_result_at", 0);
		senate_result = extras.getString("senate_result");
		senate_result_at = extras.getLong("senate_result_at", 0);
		passed = extras.getBoolean("passed", false);
		passed_at = extras.getLong("passed_at", 0);
		vetoed = extras.getBoolean("vetoed", false);
		vetoed_at = extras.getLong("vetoed_at", 0);
		override_house_result = extras.getString("override_house_result");
		override_house_result_at = extras.getLong("override_house_result_at", 0);
		override_senate_result = extras.getString("override_senate_result");
		override_senate_result_at = extras.getLong("override_senate_result_at", 0);
		awaiting_signature = extras.getBoolean("awaiting_signature", false);
		awaiting_signature_since = extras.getLong("awaiting_signature_since", 0);
		enacted = extras.getBoolean("enacted", false);
		enacted_at = extras.getLong("enacted_at", 0);
		
		sponsor_id = extras.getString("sponsor_id");
		sponsor_title = extras.getString("sponsor_title");
		sponsor_state = extras.getString("sponsor_state");
		sponsor_party = extras.getString("sponsor_party");
		sponsor_first_name = extras.getString("sponsor_first_name");
		sponsor_nickname = extras.getString("sponsor_nickname");
		sponsor_last_name = extras.getString("sponsor_last_name");
		
		setupControls();
		setupTabs();
	}
	
	public void setupControls() {
		((TextView) findViewById(R.id.title_text)).setText(Bill.formatCode(code));
	}
	
	public void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		tabHost.addTab(tabHost.newTabSpec("info_tab").setIndicator("Details", res.getDrawable(R.drawable.tab_profile)).setContent(detailsIntent()));
		tabHost.addTab(tabHost.newTabSpec("history_tab").setIndicator("History", res.getDrawable(R.drawable.tab_news)).setContent(historyIntent()));
		tabHost.setCurrentTab(0);
	}
	
	public Intent detailsIntent() {
		return new Intent(this, BillInfo.class)
			.putExtra("id", id)
			.putExtra("type", type)
			.putExtra("number", number)
			.putExtra("session", session)
			.putExtra("code", code)
			.putExtra("short_title", short_title)
			.putExtra("official_title", official_title)
			.putExtra("introduced_at", introduced_at)
			.putExtra("house_result", house_result)
			.putExtra("senate_result", senate_result)
			.putExtra("passed", passed)
			.putExtra("vetoed", vetoed)
			.putExtra("override_house_result", override_house_result)
			.putExtra("override_senate_result", override_senate_result)
			.putExtra("awaiting_signature", awaiting_signature)
			.putExtra("enacted", enacted)
			.putExtra("house_result_at", house_result_at)
			.putExtra("senate_result_at", senate_result_at)
			.putExtra("passed_at", passed_at)
			.putExtra("vetoed_at", vetoed_at)
			.putExtra("override_house_result_at", override_house_result_at)
			.putExtra("override_senate_result_at", override_senate_result_at)
			.putExtra("awaiting_signature_since", awaiting_signature_since)
			.putExtra("enacted_at", enacted_at)
			.putExtra("sponsor_id", sponsor_id)
			.putExtra("sponsor_party", sponsor_party)
			.putExtra("sponsor_state", sponsor_state)
			.putExtra("sponsor_title", sponsor_title)
			.putExtra("sponsor_first_name", sponsor_first_name)
			.putExtra("sponsor_nickname", sponsor_nickname)
			.putExtra("sponsor_last_name", sponsor_last_name);
	}
	
	public Intent historyIntent() {
		return new Intent(this, BillHistory.class)
			.putExtra("id", id);
	}
}
