package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		setupControls();
	}
	
	public void setupControls() {
		
	}
}