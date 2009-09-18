package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class LegislatorTwitter extends Activity {
	private String username;	
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.legislator_twitter);
    	
    	username = getIntent().getStringExtra("username");
    	
    	TextView status = (TextView) findViewById(R.id.first_status);
    	status.setText(username);
	}
	
}