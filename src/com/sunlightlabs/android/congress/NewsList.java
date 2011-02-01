package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadNewsTask;
import com.sunlightlabs.android.congress.tasks.LoadNewsTask.LoadsNews;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.google.news.NewsItem;

public class NewsList extends ListActivity implements LoadsNews {
	private static final int MENU_VIEW = 0;
	private static final int MENU_COPY = 1;

	private String searchTerm;
	private List<NewsItem> items;
	private LoadNewsTask loadNewsTask;
	
	private Footer footer;
	private GoogleAnalyticsTracker tracker;
	
	private String subscriptionId, subscriptionName, subscriptionClass;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer);

		Bundle extras = getIntent().getExtras();
		searchTerm = extras.getString("searchTerm");
		subscriptionId = extras.getString("subscriptionId");
		subscriptionName = extras.getString("subscriptionName");
		subscriptionClass = extras.getString("subscriptionClass");

		NewsListHolder holder = (NewsListHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			items = holder.items;
			loadNewsTask = holder.loadNewsTask;
			footer = holder.footer;
		}

		setupControls();
		
		tracker = Analytics.start(this);
		
		if (footer != null)
			footer.onScreenLoad(this, tracker);
		else
			footer = Footer.from(this, tracker);

		if (loadNewsTask != null)
			loadNewsTask.onScreenLoad(this);
		else
			loadNews();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new NewsListHolder(items, loadNewsTask, footer);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (items != null)
			setupSubscription();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Analytics.stop(tracker);
	}

	private void setupControls() {
		TextView header = (TextView) LayoutInflater.from(this).inflate(R.layout.list_header_simple, null);
		header.setText(R.string.google_news_branding);
		getListView().addHeaderView(header, null, false);
		
		Utils.setLoading(this, R.string.news_loading);
		((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				items = null;
				Utils.showLoading(NewsList.this);
				loadNews();
			}
		});

		registerForContextMenu(getListView());
	}

	private void setupSubscription() {
		footer.init(new Subscription(subscriptionId, subscriptionName, subscriptionClass, searchTerm), items);
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
		if (items == null) {
			String apiKey = getResources().getString(R.string.google_news_key);
			String referer = getResources().getString(R.string.google_news_referer);
			loadNewsTask = (LoadNewsTask) new LoadNewsTask(this).execute(searchTerm, apiKey, referer);
		} else
			displayNews();
	}

	protected void displayNews() {
		if (items != null && items.size() > 0)
			setListAdapter(new NewsAdapter(this, items));
		else
			Utils.showRefresh(this, R.string.news_empty);
		
		setupSubscription();
	}

	protected class NewsAdapter extends ArrayAdapter<NewsItem> {
		LayoutInflater inflater;

		public NewsAdapter(Activity context, List<NewsItem> items) {
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

			SimpleDateFormat format = new SimpleDateFormat("MMM dd");
			
			((TextView) view.findViewById(R.id.news_item_title)).setText(item.title);
			((TextView) view.findViewById(R.id.news_item_summary))
				.setText(Html.fromHtml(item.summary));
			((TextView) view.findViewById(R.id.news_item_when_where))
				.setText(format.format(item.timestamp.getTime()) + ", " + item.source);

			return view;
		}

	}

	static class NewsListHolder {
		List<NewsItem> items;
		LoadNewsTask loadNewsTask;
		Footer footer;
		
		public NewsListHolder(List<NewsItem> items, LoadNewsTask loadNewsTask, Footer footer) {
			this.items = items;
			this.loadNewsTask = loadNewsTask;
			this.footer = footer;
		}
	}

	public void onLoadNews(List<NewsItem> news) {
		this.items = news;
		displayNews();
		loadNewsTask = null;
	}

	public void onLoadNews(CongressException e) {
		this.onLoadNews((List<NewsItem>) null);
		Utils.showRefresh(this, e.getMessage());		
	}

	public Context getContext() {
		return this;
	}
}
