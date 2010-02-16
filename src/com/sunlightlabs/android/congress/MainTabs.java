package com.sunlightlabs.android.congress;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class MainTabs extends TabActivity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tabs);
        setupTabs();
    }
    
    public void setupTabs() {
    	TabHost tabHost = getTabHost();
		
    	tabHost.addTab(tabHost.newTabSpec("bills_tab").setIndicator("Bills").setContent(billsIntent()));
		tabHost.addTab(tabHost.newTabSpec("people_tab").setIndicator("People").setContent(peopleIntent()));
		
		tabHost.setCurrentTab(0);
    }
    
    public Intent billsIntent() {
    	return new Intent(this, MainBills.class);
    }
    
    public Intent peopleIntent() {
    	return new Intent(this, MainLegislators.class);
    }
    
}