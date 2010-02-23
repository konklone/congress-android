package com.sunlightlabs.android.congress;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ScrollView;
import android.widget.TabHost;

public class MainTabs extends TabActivity {
	
	private static final int FIRST = 1;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tabs);
        setupTabs();
        
        if (firstTime())
        	showDialog(FIRST);
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
    
    public boolean firstTime() {
		if (Preferences.getBoolean(this, "first_time", true)) {
			Preferences.setBoolean(this, "first_time", false);
			return true;
		}
		return false;
	}
    
    @Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	LayoutInflater inflater = getLayoutInflater();
    	
        switch(id) {
        case FIRST:
        	ScrollView firstView = (ScrollView) inflater.inflate(R.layout.first_time, null);
        	
        	builder.setView(firstView);
        	builder.setPositiveButton(R.string.first_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
            return builder.create();
        default:
            return null;
        }
    }
    
}