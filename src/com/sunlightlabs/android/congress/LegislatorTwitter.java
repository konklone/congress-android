package com.sunlightlabs.android.congress;

import java.util.List;
import java.util.ListIterator;

import com.sunlightlabs.entities.Legislator;

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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class LegislatorTwitter extends ListActivity {
	private String username;
	static final int LOADING_TWEETS = 0;
    ProgressThread progressThread;
    ProgressDialog progressDialog;
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	username = getIntent().getStringExtra("username");
    	
    	showDialog(LOADING_TWEETS);
	}
   
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOADING_TWEETS:
            progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Plucking tweets from the air...");
            progressThread = new ProgressThread(handler);
            progressThread.start();
            return progressDialog;
        default:
            return null;
        }
    }

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

    /** Nested class that performs progress calculations (counting) */
    private class ProgressThread extends Thread {
        Handler mHandler;
       
        ProgressThread(Handler h) {
            mHandler = h;
        }
       
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
            
            Message msg = mHandler.obtainMessage();
            msg.setData(b);
            mHandler.sendMessage(msg);
    	}
    }

}