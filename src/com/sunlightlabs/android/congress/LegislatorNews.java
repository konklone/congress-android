package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
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
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.sunlightlabs.android.yahoo.news.NewsException;
import com.sunlightlabs.android.yahoo.news.NewsItem;
import com.sunlightlabs.android.yahoo.news.NewsService;

public class LegislatorNews extends ListActivity {
	private static final int MENU_VIEW = 0;
	private static final int MENU_COPY = 1;
	
	private String searchName;
	private NewsItem[] items = null;

	private LoadNewsTask loadNewsTask = null;
	private ProgressDialog dialog = null;
	
	private Button refresh;
    	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.news_list);
    	
    	searchName = getIntent().getStringExtra("searchName");
    	searchName = correctExceptions(searchName);
    	
    	LegislatorNewsHolder holder = (LegislatorNewsHolder) getLastNonConfigurationInstance();
    	if (holder != null) {
    		items = holder.items;
    		loadNewsTask = holder.loadNewsTask;
    		if (loadNewsTask != null) {
    			loadNewsTask.context = this;
    			loadingDialog();
    		}
    	}
    	
    	setupControls();
    	if (loadNewsTask == null)
    		loadNews();
	}
	
	@Override
    public Object onRetainNonConfigurationInstance() {
		LegislatorNewsHolder holder = new LegislatorNewsHolder();
		holder.items = this.items;
		holder.loadNewsTask = this.loadNewsTask;
    	return holder;
    }
	
    @Override
	public void onListItemClick(ListView parent, View view, int position, long id) {
		NewsItem item = (NewsItem) parent.getItemAtPosition(position);
		launchNews(item);
	}
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, MENU_VIEW, 0, "View");
		menu.add(0, MENU_COPY, 1, "Copy link");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		NewsItem newsItem = (NewsItem) getListView().getItemAtPosition(info.position);
		
		switch (item.getItemId()) {
		case MENU_VIEW:
			launchNews(newsItem);
			return true;
		case MENU_COPY:
			ClipboardManager cm = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
			cm.setText(newsItem.clickURL);
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void launchNews(NewsItem item) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.clickURL)));
	}
    
    private void setupControls() {
    	refresh = (Button) this.findViewById(R.id.news_refresh);
    	refresh.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loadNews();
			}
		});
    	registerForContextMenu(getListView());
    }
    
    protected void displayNews() {
    	TextView empty = (TextView) findViewById(R.id.news_empty);
    	if (items != null) {
    		setListAdapter(new NewsAdapter(this, items));
    		if (items.length <= 0) {
        		empty.setText(R.string.news_empty);
        		refresh.setVisibility(View.VISIBLE);
        	}
		} else { 
			empty.setText(R.string.connection_failed);
    		refresh.setVisibility(View.VISIBLE);
		}
    }
	
	protected void loadNews() {
	    if (items == null)
    		new LoadNewsTask(this).execute(searchName);
		else
			displayNews();
	}
    
    private void loadingDialog() {
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Plucking news from the air...");
        dialog.show();
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
	
	private class LoadNewsTask extends AsyncTask<String,Void,NewsItem[]> {
		public LegislatorNews context;
		
		public LoadNewsTask(LegislatorNews context) {
			super();
			this.context = context;
			this.context.loadNewsTask = this;
		}
		
		@Override
		protected void onPreExecute() {
			context.loadingDialog();
		}
		
		@Override
		protected NewsItem[] doInBackground(String... searchTerm) {
			try {
    			String apiKey = context.getResources().getString(R.string.yahoo_news_key);
    			return new NewsService(apiKey).fetchNewsResults(searchTerm[0]);
    		} catch (NewsException e) {
    			return null;
    		}
		}
		
		@Override
		protected void onPostExecute(NewsItem[] items) {
			if (context.dialog != null && context.dialog.isShowing())
    			context.dialog.dismiss();
    		
    		context.items = items;
    		context.displayNews();
    		context.loadNewsTask = null;
		}
	}
	
	static class LegislatorNewsHolder{
		NewsItem[] items;
		LoadNewsTask loadNewsTask;
	}
}