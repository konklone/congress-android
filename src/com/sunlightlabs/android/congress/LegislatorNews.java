package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.ListActivity;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.yahoo.news.NewsException;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.yahoo.news.NewsService;

public class LegislatorNews extends ListActivity {
	private static final int MENU_VIEW = 0;
	private static final int MENU_COPY = 1;
	
	private String searchName;
	private NewsItem[] items = null;

	private LoadNewsTask loadNewsTask = null;
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.list);
    	
    	Bundle extras = getIntent().getExtras();
    	String title = extras.getString("title");
    	String firstName = extras.getString("firstName");
    	String nickname = extras.getString("nickname");
    	String lastName = extras.getString("lastName");
    	searchName = LegislatorNews.searchNameFor(title, firstName, nickname, lastName); 
    	searchName = LegislatorNews.correctExceptions(searchName);
    	
    	LegislatorNewsHolder holder = (LegislatorNewsHolder) getLastNonConfigurationInstance();
    	if (holder != null) {
    		items = holder.items;
    		loadNewsTask = holder.loadNewsTask;
    		if (loadNewsTask != null)
    			loadNewsTask.onScreenLoad(this);
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
	
	private void setupControls() {
		Utils.setLoading(this, R.string.news_loading);
    	((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				items = null;
				Utils.showLoading(LegislatorNews.this);
				loadNews();
			}
		});
    	
    	registerForContextMenu(getListView());
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
    
    protected void loadNews() {
	    if (items == null)
    		loadNewsTask = (LoadNewsTask) new LoadNewsTask(this).execute(searchName);
		else
			displayNews();
	}
    
    protected void displayNews() {
    	if (items != null && items.length > 0)
    		setListAdapter(new NewsAdapter(this, items));
    	else
    		Utils.showRefresh(this, R.string.news_empty);
    }
    
    private static String searchNameFor(String title, String firstName, String nickname, String lastName) {
    	String first;
    	if (nickname != null && !nickname.equals(""))
    		first = nickname;
    	else
    		first = firstName;
    	return title + ". " + first + " " + lastName;
    }
    
    private static String correctExceptions(String name) {
		if (name.equals("Rep. Nancy Pelosi"))
			return "Speaker Nancy Pelosi";
		else if (name.equals("Del. Eleanor Norton"))
			return "Eleanor Holmes Norton";
		else
			return name;
	}
	
	protected class NewsAdapter extends ArrayAdapter<NewsItem> {
    	LayoutInflater inflater;

        public NewsAdapter(Activity context, NewsItem[] items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
        }
        
        @Override
        public boolean areAllItemsEnabled() {
        	return true;
        }
        
        @Override
        public int getViewTypeCount() {
        	return 1;
        }

		public View getView(int position, View view, ViewGroup parent) {
			if (view == null)
				view = inflater.inflate(R.layout.news_item, null);
			
			NewsItem item = getItem(position);
			
			((TextView) view.findViewById(R.id.news_item_title)).setText(item.title);
			((TextView) view.findViewById(R.id.news_item_summary)).setText(item.summary);
			((TextView) view.findViewById(R.id.news_item_when_where))
				.setText(item.timestamp.format("%b %d") + ", " + item.source);
			
			return view;
		}

    }
	
	private class LoadNewsTask extends AsyncTask<String,Void,NewsItem[]> {
		public LegislatorNews context;
		
		public LoadNewsTask(LegislatorNews context) {
			super();
			this.context = context;
		}
		
		public void onScreenLoad(LegislatorNews context) {
			this.context = context;
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