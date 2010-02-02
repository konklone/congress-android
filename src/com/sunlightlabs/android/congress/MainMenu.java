package com.sunlightlabs.android.congress;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class MainMenu extends TabActivity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tabs);
        setupTabs();
    }
    
    public void setupTabs() {
    	TabHost tabHost = getTabHost();
		
    	tabHost.addTab(tabHost.newTabSpec("activity_tab").setIndicator("Activity").setContent(activityIntent()));
		tabHost.addTab(tabHost.newTabSpec("people_tab").setIndicator("People").setContent(peopleIntent()));
		
		tabHost.setCurrentTab(0);
    }
    
    public Intent activityIntent() {
    	return new Intent(this, MainActivity.class);
    }
    
    public Intent peopleIntent() {
    	return new Intent(this, MainLegislators.class);
    }
    
}