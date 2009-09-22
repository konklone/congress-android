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
	private String username;
	static final int LOADING_TWEETS = 0;
    
	Thread twitterThread;
    ProgressDialog progressDialog;
    
    // Define the Handler that receives messages from the thread and update the progress
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	Bundle data = msg.getData();
            boolean total = data.getBoolean("success");
            if (total) {
            	String[] statuses = data.getStringArray("statuses");
            	
            	setListAdapter(new ArrayAdapter<String>(LegislatorTwitter.this, android.R.layout.simple_list_item_1, statuses));
            	getListView().setOnItemClickListener(new OnItemClickListener() { 
            		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            			String tweet = ((String) parent.getItemAtPosition(position));
            			Toast.makeText(LegislatorTwitter.this, tweet, 5);
            		}
            	});
            	
            	dismissDialog(LOADING_TWEETS);
            }
        }
    };
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	username = getIntent().getStringExtra("username");
    	
    	loadTweets();
	}
	
	protected void loadTweets() {
		twitterThread = new Thread() {
	        public void run() { 
	        	Twitter twitter = new Twitter();
	        	List<Status> statuses;
	        	try {
	        		statuses = twitter.getUserTimeline(username);
	        		returnStatuses(statuses);
	        	} catch(TwitterException e) {
	        		Log.e("ERROR", e.getMessage());
	        	}
	        }
	    
	    	public void returnStatuses(List<Status> statuses) {
	            Bundle b = new Bundle();
	            b.putBoolean("success", true);
	            
	            String[] tweets = new String[statuses.size()];
	            int i = 0;
	            for (ListIterator<Status> it = statuses.listIterator(); it.hasNext();)
	            	tweets[i++] = it.next().getText();
	            
	            b.putStringArray("statuses", tweets);
	            
	            Message msg = handler.obtainMessage();
	            msg.setData(b);
	            handler.sendMessage(msg);
	    	}
	    };
		
		showDialog(LOADING_TWEETS);
	}
   
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOADING_TWEETS:
            progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Plucking tweets from the air...");
            //twitterThread = new ProgressThread();
            twitterThread.start();
            return progressDialog;
        default:
            return null;
        }
    }

}