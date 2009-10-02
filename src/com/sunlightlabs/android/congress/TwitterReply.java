package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sunlightlabs.android.twitter.Twitter;

public class TwitterReply extends Activity {
	private static final int RESULT_PREFS = 0;

	private String username, password;
	private EditText message;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.twitter_reply);
        
        username = Preferences.getString(this, "twitter_username");
        password = Preferences.getString(this, "twitter_password");
     
        setupControls();
	}
	
	public void setupControls() {
		message = (EditText) findViewById(R.id.twitter_message);
		Button reply = (Button) findViewById(R.id.twitter_ok);
		reply.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				verifyHaveCredentials();
			}
		});
		
		
		Button cancel = (Button) findViewById(R.id.twitter_cancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	// Gets called after verifyHaveCredentials, which doesn't truly verify that we have credentials - 
	// it just verifies that if we didn't have them, we brought them to the preference screen once.
	public void onHaveCredentials() {
		Twitter twitter = new Twitter(username, password);
		if (twitter.update(message.getText().toString())) {
			alert("Your Twitter status has been updated.");
			finish();
		} else
			alert("Could not update your Twitter status. Please verify your credentials in the Settings screen.");
	}
	
	public void verifyHaveCredentials() {
		if (username != null && password != null)
			onHaveCredentials();
		else {
			alert("Please enter your Twitter credentials.");
			startActivityForResult(new Intent(this, Preferences.class), RESULT_PREFS);
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case RESULT_PREFS:
			username = Preferences.getString(this, "twitter_username");
	        password = Preferences.getString(this, "twitter_password");
			// even if they hit the Back button, let's continue on and not *force* them
			onHaveCredentials();
		}
	}
	
	public void alert(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
	
}