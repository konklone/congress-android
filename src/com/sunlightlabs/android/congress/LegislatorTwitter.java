package com.sunlightlabs.android.congress;

import java.util.List;
import java.util.ListIterator;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class LegislatorTwitter extends ListActivity {
	static final int LOADING_TWEETS = 0;
	
	private String username;
	private Status[] tweets;
    	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	username = getIntent().getStringExtra("username");
    	
    	loadTweets();
	}
	
    // Define the Handler that receives messages from the thread and update the progress
    final Handler handler = new Handler();
    final Runnable updateTweets = new Runnable() {
        public void run() {
        	setListAdapter(new ArrayAdapter<Status>(LegislatorTwitter.this, android.R.layout.simple_list_item_1, tweets));
        	getListView().setOnItemClickListener(new OnItemClickListener() { 
        		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        			Status tweet = ((Status) parent.getItemAtPosition(position));
        			Toast.makeText(LegislatorTwitter.this, tweet.getText(), 5);
        		}
        	});
        	
        	dismissDialog(LOADING_TWEETS);
        }
    };
	
	protected void loadTweets() {
		Thread twitterThread = new Thread() {
	        public void run() { 
	        	try {
	        		Twitter twitter = new Twitter();
	        		List<Status> tweetList = twitter.getUserTimeline(username);
	        		tweets = (Status[]) tweetList.toArray(new Status[0]);
	        	} catch(TwitterException e) {
	        		Log.e("ERROR", e.getMessage());
	        	}
	        	handler.post(updateTweets);
	        }
	    };
	    twitterThread.start();
	    
		showDialog(LOADING_TWEETS);
	}
    
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOADING_TWEETS:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Plucking tweets from the air...");
            return dialog;
        default:
            return null;
        }
    }

}