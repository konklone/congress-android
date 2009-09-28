package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sunlightlabs.android.twitter.Status;
import com.sunlightlabs.android.twitter.Twitter;
import com.sunlightlabs.android.twitter.TwitterException;

public class LegislatorTwitter extends ListActivity {
	static final int LOADING = 0;
	
	private String username;
	private Status[] tweets;
    	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	username = getIntent().getStringExtra("username");
    	
    	loadTweets();
	}
	
    final Handler handler = new Handler();
    final Runnable updateThread = new Runnable() {
        public void run() {
        	setListAdapter(new TweetAdapter(LegislatorTwitter.this, tweets));
        	dismissDialog(LOADING);
        }
    };
	
	protected void loadTweets() {
		Thread loadingThread = new Thread() {
	        public void run() { 
	        	try {
	        		Twitter twitter = new Twitter();
	        		tweets = twitter.getUserTimeline(username);
	        	} catch(TwitterException e) {
	        		Toast.makeText(LegislatorTwitter.this, "Couldn't load tweets.", Toast.LENGTH_SHORT).show();
	        	}
	        	handler.post(updateThread);
	        }
	    };
	    loadingThread.start();
	    
		showDialog(LOADING);
	}
    
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOADING:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Plucking tweets from the air...");
            return dialog;
        default:
            return null;
        }
    }
    
    protected class TweetAdapter extends BaseAdapter {
    	private Status[] tweets;
    	LayoutInflater inflater;

        public TweetAdapter(Activity context, Status[] tweets) {
            this.tweets = tweets;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

		public int getCount() {
			return tweets.length;
		}

		public Object getItem(int position) {
			return tweets[position];
		}

		public long getItemId(int position) {
			Status tweet = (Status) getItem(position);
			return tweet.id;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view;
			if (convertView == null) {
				view = (LinearLayout) inflater.inflate(R.layout.tweet, null);
			} else {
				view = (LinearLayout) convertView;
			}
			
			Status tweet = (Status) getItem(position);
			
			TextView text = (TextView) view.findViewById(R.id.tweet_text);
			text.setText(tweet.text);
			
			TextView when = (TextView) view.findViewById(R.id.tweet_when);
			when.setText("posted " + timeAgoInWords(tweet.createdAtMillis));
			
			return view;
		}
		
		private String timeAgoInWords(long olderTime) {
			long now = System.currentTimeMillis();
			long diff = now - olderTime; 
			if (diff < 2000) // 2 seconds
				return "just now";
			else if (diff < 50000) // 50 seconds
				return (diff / 1000) + " seconds ago";
			else if (diff < 65000) // 1 minute, 5 seconds
				return "a minute ago";
			else if (diff < 3300000) // 55 minutes
				return (diff / 60000) + " minutes ago";
			else if (diff < 3900000) // 65 minutes
				return "an hour ago";
			else if (diff < 82800000) // 23 hours
				return (diff / 3600000) + " hours ago";
			else if (diff < 90000000) // 25 hours
				return "a day ago";
			else
				return (diff / 86400000) + " days ago";
		}

    }

}