package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sunlightlabs.android.twitter.Twitter;
import com.sunlightlabs.android.twitter.TwitterException;

public class TwitterReply extends Activity {
	private static final int RESULT_PREFS = 0;
	
	private static final int TWEETING = 0; 

	private String username, password;
	private String tweet_text, tweet_username;
	private EditText message;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.twitter_reply);
        
        username = Preferences.getString(this, "twitter_username");
        password = Preferences.getString(this, "twitter_password");
        
        Bundle extras = getIntent().getExtras();
        tweet_text = extras.getString("tweet_text");
        tweet_username = extras.getString("tweet_username");
     
        setupControls();
	}
	
	public void setupControls() {		
		TextView original = (TextView) findViewById(R.id.twitter_original);
		original.setText(tweet_text);
		
		message = (EditText) findViewById(R.id.twitter_message);
		message.setText("@" + tweet_username + " ");
		
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
		postTweet();
	}
	
	final Handler handler = new Handler();
    final Runnable postSucceeded = new Runnable() {
        public void run() {
        	alert("Your Twitter status has been updated.");
        	removeDialog(TWEETING);
			finish();
        }
    };
    final Runnable postFailed = new Runnable() {
    	public void run() {
    		alert("Could not update your Twitter status. Please verify your credentials in the Settings screen.");
    		removeDialog(TWEETING);
    	}
    };
    final Runnable postError = new Runnable() {
    	public void run() {
    		alert("Could not connect to Twitter. Please try again later.");
    		removeDialog(TWEETING);
    	}
    };
    
	
	public void postTweet() {
		Thread poster = new Thread() {
			public void run() {
				Twitter twitter = new Twitter(username, password);
				try {
					if (twitter.update(message.getText().toString()))
						handler.post(postSucceeded);
					else
						handler.post(postFailed);
				} catch(TwitterException e) {
					handler.post(postError);
				}
			}
		};
		poster.run();
		
		showDialog(TWEETING);
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
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
	protected Dialog onCreateDialog(int id) {
        switch(id) {
        case TWEETING:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Updating your status...");
            return dialog;
        default:
            return null;
        }
    }
	
}