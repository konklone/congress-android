package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.format.Time;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.sunlightlabs.android.twitter.Status;
import com.sunlightlabs.android.twitter.Twitter;
import com.sunlightlabs.android.twitter.TwitterException;

public class LegislatorTwitter extends ListActivity {
	private static final int LOADING = 0;
	
	private static final int MENU_REPLY = 0;
	private static final int MENU_COPY = 1;
	
	private String username;
	private Status[] tweets;
	
	private Button refresh;
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.twitter);
    	
    	username = getIntent().getStringExtra("username");
    	
    	setupControls();
    	
    	loadTweets();
	}
	
    final Handler handler = new Handler();
    final Runnable updateThread = new Runnable() {
        public void run() {        	
        	setListAdapter(new TweetAdapter(LegislatorTwitter.this, tweets));
        	
        	if (tweets.length <= 0) {
        		TextView empty = (TextView) LegislatorTwitter.this.findViewById(R.id.twitter_empty);
        		empty.setText(R.string.twitter_empty);
        		refresh.setVisibility(View.VISIBLE);
        	}
        	
        	removeDialog(LOADING);
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
	
	public void onListItemClick(ListView parent, View v, int position, long id) {
		Status tweet = (Status) parent.getItemAtPosition(position);
		launchReplyForTweet(tweet);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, MENU_REPLY, 0, "Reply");
		menu.add(0, MENU_COPY, 1, "Copy tweet text");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Status tweet = (Status) getListView().getItemAtPosition(info.position);
		
		switch (item.getItemId()) {
		case MENU_REPLY:
			launchReplyForTweet(tweet);
			return true;
		case MENU_COPY:
			ClipboardManager cm = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
			cm.setText(tweet.text);
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void launchReplyForTweet(Status tweet) {
		Intent intent = new Intent(this, TwitterReply.class);
		intent.putExtra("tweet_text", tweet.text);
		intent.putExtra("tweet_username", tweet.username);
		startActivity(intent);
	}
	
	private void setupControls() {
		refresh = (Button) this.findViewById(R.id.twitter_refresh);
    	refresh.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loadTweets();
			}
		});
    	registerForContextMenu(getListView());
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
			
			TextView byline = (TextView) view.findViewById(R.id.tweet_byline);
			byline.setText("posted " + timeAgoInWords(tweet.createdAtMillis) + " by @" + tweet.username);
			
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
			else if (diff < 1123200000) // 13 days
				return (diff / 86400000) + " days ago";
			else {
				Time old = new Time();
				old.set(olderTime);
				return old.format("%b %d");
			}
		}

    }

}