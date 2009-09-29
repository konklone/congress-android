package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.sunlightlabs.android.yahoo.news.NewsException;
import com.sunlightlabs.android.yahoo.news.NewsItem;
import com.sunlightlabs.android.yahoo.news.NewsService;

public class LegislatorNews extends ListActivity {
	private static final int LOADING = 0;
	
	private String searchName;
	private NewsItem[] items;
    	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	searchName = getIntent().getStringExtra("searchName");
    	searchName = correctExceptions(searchName);
    	
    	loadNews();
	}
	
	final Handler handler = new Handler();
    final Runnable updateThread = new Runnable() {
        public void run() {
        	setListAdapter(new NewsAdapter(LegislatorNews.this, items));
        	getListView().setOnItemClickListener(new OnItemClickListener() { 
        		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        			NewsItem item = (NewsItem) parent.getItemAtPosition(position);
        			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.clickURL)));
        		}
        	});
        	dismissDialog(LOADING);
        }
    };
	
	protected void loadNews() {
		Thread loadingThread = new Thread() {
	        public void run() { 
	        	try {
	    			String apiKey = getResources().getString(R.string.yahoo_news_key);
	    			NewsService service = new NewsService(apiKey);
	    			items = service.fetchNewsResults(searchName);
	    		} catch (NewsException e) {
	    			Toast.makeText(LegislatorNews.this, "Couldn't load news.", Toast.LENGTH_SHORT).show();
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
            dialog.setMessage("Plucking news from the air...");
            return dialog;
        default:
            return null;
        }
    }
	
	private String correctExceptions(String name) {
		if (name.equals("Rep. Nancy Pelosi"))
			return "Speaker Nancy Pelosi";
		else if (name.equals("Del. Eleanor Norton"))
			return "Eleanor Holmes Norton";
		else
			return name;
	}
	
	protected class NewsAdapter extends BaseAdapter {
    	private NewsItem[] items;
    	LayoutInflater inflater;

        public NewsAdapter(Activity context, NewsItem[] items) {
            this.items = items;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

		public int getCount() {
			return items.length;
		}

		public Object getItem(int position) {
			return items[position];
		}

		public long getItemId(int position) {
			return ((long) position);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view;
			if (convertView == null) {
				view = (LinearLayout) inflater.inflate(R.layout.news_item, null);
			} else {
				view = (LinearLayout) convertView;
			}
			
			NewsItem item = (NewsItem) getItem(position);
			
			
			TextView text = (TextView) view.findViewById(R.id.news_item_title);
			text.setText(item.title);
			
			TextView summary = (TextView) view.findViewById(R.id.news_item_summary);
			summary.setText(item.summary);
			
			TextView when = (TextView) view.findViewById(R.id.news_item_when_where);
			String time = item.timestamp.format("%b %d");
			when.setText(time + ", " + item.source);
			
			return view;
		}

    }
}