package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    final Runnable updateNews = new Runnable() {
        public void run() {
        	setListAdapter(new NewsAdapter(LegislatorNews.this, items));
        	dismissDialog(LOADING);
        }
    };
	
	protected void loadNews() {
		Thread newsThread = new Thread() {
	        public void run() { 
	        	try {
	    			String apiKey = getResources().getString(R.string.yahoo_news_key);
	    			NewsService service = new NewsService(apiKey);
	    			items = service.fetchNewsResults(searchName);
	    		} catch (NewsException e) {
	    			Toast.makeText(LegislatorNews.this, "Couldn't load news, try again later.", Toast.LENGTH_SHORT);
	    		}
	        	handler.post(updateNews);
	        }
	    };
	    newsThread.start();
	    
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
	
	public NewsItem[] fakeNews() {
		NewsItem[] items = new NewsItem[5];
		
		items[0] = new NewsItem("Title of the Article 1", "BBC", "http://bbc.co.uk", "http://sunlightlabs.com", new Time());
		items[1] = new NewsItem("Title of the Article 2", "NBC News", "http://news.nbc.com/anything/everything", "http://sunlightlabs.com", new Time());
		items[2] = new NewsItem("Title 3", "BBC", "http://bbc.co.uk", "http://sunlightlabs.com", new Time());
		items[3] = new NewsItem("Title of the Article 4", "NBC News", "http://news.nbc.com/anything/everything", "http://sunlightlabs.com", new Time());
		items[4] = new NewsItem("Hugely Long Major Title of the Article 5", "BBC", "http://bbc.co.uk", "http://sunlightlabs.com", new Time());
		
		return items;
	}
	
	private String correctExceptions(String name) {
		String newName;
		if (name == "Rep Nancy Pelosi")
			newName = "Speaker Nancy Pelosi";
		else
			newName = name;
		return newName;
	}
	
	protected class NewsAdapter extends BaseAdapter {
    	private Activity context;
    	private NewsItem[] items;
    	LayoutInflater inflater;

        public NewsAdapter(Activity context, NewsItem[] items) {
            this.context = context;
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
				view = (LinearLayout) inflater.inflate(R.layout.legislator_news_item, null);
			} else {
				view = (LinearLayout) convertView;
			}
			
			NewsItem item = (NewsItem) getItem(position);
			
			TextView text = (TextView) view.findViewById(R.id.news_item_title);
			text.setText(item.title);
			TextView when = (TextView) view.findViewById(R.id.news_item_when);
			when.setText(item.timestamp.toString());
			
			return view;
		}

    }
}