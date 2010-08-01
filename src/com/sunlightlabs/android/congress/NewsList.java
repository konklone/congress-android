package com.sunlightlabs.android.congress;

import java.util.ArrayList;

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

import com.sunlightlabs.android.congress.Footer.OnFooterClickListener;
import com.sunlightlabs.android.congress.Footer.State;
import com.sunlightlabs.android.congress.notifications.Notifications;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.yahoo.news.NewsException;
import com.sunlightlabs.yahoo.news.NewsItem;
import com.sunlightlabs.yahoo.news.NewsService;

public class NewsList extends ListActivity implements OnFooterClickListener {
	private static final String NOTIFICATION_TYPE = "news";

	private static final int MENU_VIEW = 0;
	private static final int MENU_COPY = 1;
	
	private String searchTerm;
	private ArrayList<NewsItem> items = null;

	private LoadNewsTask loadNewsTask = null;
	
	private Database database;
	private String entityId, entityType, entityName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer);
    	
		database = new Database(this);
		database.open();

		Intent i = getIntent();
		entityId = i.getStringExtra("entityId");
		entityName = i.getStringExtra("entityName");
		entityType = i.getStringExtra("entityType");
		searchTerm = i.getStringExtra("searchTerm");
    	
    	NewsListHolder holder = (NewsListHolder) getLastNonConfigurationInstance();
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
		NewsListHolder holder = new NewsListHolder();
		holder.items = this.items;
		holder.loadNewsTask = this.loadNewsTask;
    	return holder;
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}

	private void setupControls() {
		Utils.setLoading(this, R.string.news_loading);
    	((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				items = null;
				Utils.showLoading(NewsList.this);
				loadNews();
			}
		});

		registerForContextMenu(getListView());
    	
		setupFooter();
    }
	
	private void setupFooter() {
		Footer footer = (Footer) findViewById(R.id.footer);
		footer.setListener(this);
		footer.setHasEntity(true);
		footer.setEntityId(entityId);
		footer.setEntityName(entityName);
		footer.setEntityType(entityType);
		footer.setNotificationType(NOTIFICATION_TYPE);
		footer.setDatabase(database);

		// if the service is started, check the database
		if (Utils.getBooleanPreference(this, Preferences.KEY_NOTIFY_ENABLED,
				Preferences.DEFAULT_NOTIFY_ENABLED)
				&& Database.NOTIFICATIONS_ON.equals(database.getNotificationStatus(entityId, NOTIFICATION_TYPE)))
			footer.setOn();
		else
			footer.setOff();
	}

	public void onFooterClick(Footer footer, State state) {
		if (state == State.ON) {

			// if notifications are not yet enabled, send broadcast to start
			// them
			if (!Utils.getBooleanPreference(this, Preferences.KEY_NOTIFY_ENABLED,
					Preferences.DEFAULT_NOTIFY_ENABLED)) {

				Utils.setBooleanPreference(this, Preferences.KEY_NOTIFY_ENABLED, true);
				Notifications.startNotificationsBroadcast(this);
			}
		}
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
    		loadNewsTask = (LoadNewsTask) new LoadNewsTask(this).execute(searchTerm);
		else
			displayNews();
	}
    
    protected void displayNews() {
    	if (items != null && items.size() > 0)
    		setListAdapter(new NewsAdapter(this, items));
    	else
    		Utils.showRefresh(this, R.string.news_empty);
    }
    
    protected class NewsAdapter extends ArrayAdapter<NewsItem> {
    	LayoutInflater inflater;

        public NewsAdapter(Activity context, ArrayList<NewsItem> items) {
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

		@Override
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
	
	private class LoadNewsTask extends AsyncTask<String,Void,ArrayList<NewsItem>> {
		public NewsList context;
		
		public LoadNewsTask(NewsList context) {
			super();
			this.context = context;
		}
		
		public void onScreenLoad(NewsList context) {
			this.context = context;
		}
		
		@Override
		protected ArrayList<NewsItem> doInBackground(String... searchTerm) {
			try {
    			String apiKey = context.getResources().getString(R.string.yahoo_news_key);
    			return new NewsService(apiKey).fetchNewsResults(searchTerm[0]);
    		} catch (NewsException e) {
    			return null;
    		}
		}
		
		@Override
		protected void onPostExecute(ArrayList<NewsItem> items) {    		
    		context.items = items;
    		context.displayNews();
    		context.loadNewsTask = null;
		}
	}
	
	static class NewsListHolder{
		ArrayList<NewsItem> items;
		LoadNewsTask loadNewsTask;
	}
}