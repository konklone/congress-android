package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Window;

public class CreateShortcut extends Activity {

	public static final int RESULT_LASTNAME = 2;
	private static final int RESULT_SHORTCUT = 10;
	
	private boolean running = false;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        if (savedInstanceState != null)
        	running = savedInstanceState.getBoolean("running", false);
        
        if (!running)
        	getResponse();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("running", running);
	}
	
	public void getResponse() {
		running = true;
		
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.GetText");
		Bundle extras = new Bundle();
		
		extras.putString("ask", "Enter a last name:");
		extras.putString("hint", "e.g. Schumer");
		extras.putInt("inputType", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		
		intent.putExtras(extras);
		startActivityForResult(intent, RESULT_LASTNAME);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case RESULT_LASTNAME:
			if (resultCode == RESULT_OK) {
				String lastName = data.getExtras().getString("response");
				if (!lastName.equals("")) {
					Bundle extras = new Bundle();
					extras.putString("last_name", lastName);
					Intent i = new Intent();
					i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
					extras.putBoolean("shortcut", true);
					i.putExtras(extras);
					startActivityForResult(i, RESULT_SHORTCUT);
				} else
					finish();
			} else
				finish();
			break;
		case RESULT_SHORTCUT:
			if (resultCode == RESULT_OK) {
				setResult(RESULT_OK, data);
	    		finish();
			}
			break;
		}
	}
	
}