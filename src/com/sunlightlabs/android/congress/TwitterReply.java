package com.sunlightlabs.android.congress;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class TwitterReply extends Activity {
	private static final int RESULT_PREFS = 0;

	private String username, password;
	private String tweet_text, tweet_username;
	private long tweet_in_reply_to_id;
	private EditText message;
	
	private UpdateTwitterTask updateTwitterTask = null;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.twitter_reply);
        
        username = Preferences.getString(this, "twitter_username");
        password = Preferences.getString(this, "twitter_password");
        
        Bundle extras = getIntent().getExtras();
        tweet_text = extras.getString("tweet_text");
        tweet_username = extras.getString("tweet_username");
        tweet_in_reply_to_id = extras.getLong("tweet_in_reply_to_id");
     
        setupControls();
        
        updateTwitterTask = (UpdateTwitterTask) getLastNonConfigurationInstance();
        if (updateTwitterTask != null)
        	updateTwitterTask.onScreenLoad(this);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return updateTwitterTask;
	}
	
	public void setupControls() {		
		((TextView) findViewById(R.id.twitter_original)).setText(tweet_text);
		
		message = (EditText) findViewById(R.id.twitter_message);
		message.setText("@" + tweet_username + " ");
		
		Button reply = (Button) findViewById(R.id.twitter_ok);
		reply.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (updateTwitterTask == null)
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
		updateTwitterTask = (UpdateTwitterTask) new UpdateTwitterTask(this).execute();
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
	
	private class UpdateTwitterTask extends AsyncTask<Void,Void,Integer> {
		public TwitterReply context;
		private ProgressDialog dialog;
		
		private final int SUCCESS = 0;
		private final int FAILURE = 1;
		private final int ERROR = 2;
		
		public UpdateTwitterTask(TwitterReply context) {
			super();
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			loadingDialog();
		}
		
		public void onScreenLoad(TwitterReply context) {
			this.context = context;
			loadingDialog();
		}
		
		@Override
		protected Integer doInBackground(Void... nothing) {
			Twitter twitter = new Twitter(username, password);
			// until we use something that implements OAuth, and then register with Twitter,
			// the source is going to just appear as "web". But that's better than "JTwitter".
			twitter.setSource("Congress on Android");
			try {
				twitter.setStatus(message.getText().toString(), tweet_in_reply_to_id);
				return new Integer(SUCCESS);
			} catch(TwitterException.E403 e) {
				return new Integer(FAILURE);
			} catch(TwitterException e) {
				return new Integer(ERROR);
			}
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
			
			switch(result.intValue()) {
			case SUCCESS:
				context.alert("Your Twitter status has been updated.");
				context.finish();
				break;
			case ERROR:
				context.alert("Could not connect to Twitter. Please try again later.");
				break;
			case FAILURE:
				context.alert("Could not update your Twitter status, there was an issue with your credentials. Please verify them in the Twitter Settings screen.");
				break;
			}
			
			context.updateTwitterTask = null;
		}
		
		private void loadingDialog() {
	        dialog = new ProgressDialog(context);
	        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	        dialog.setMessage("Updating your status...");
	        dialog.setCancelable(false);
	        dialog.show();
	    }
	}
	
}