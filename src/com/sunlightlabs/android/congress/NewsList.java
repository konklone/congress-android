package com.sunlightlabs.android.congress;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
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

import com.sunlightlabs.android.congress.notifications.NotificationEntity;
import com.sunlightlabs.android.congress.tasks.LoadYahooNewsTask;
import com.sunlightlabs.android.congress.tasks.LoadYahooNewsTask.LoadsYahooNews;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.yahoo.news.NewsItem;

public class NewsList extends ListActivity implements LoadsYahooNews {
	private static final int MENU_VIEW = 0;
	private static final int MENU_COPY = 1;

	private String searchTerm;
	private ArrayList<NewsItem> items = null;

	private LoadYahooNewsTask loadNewsTask = null;

	private NotificationEntity entity;
	private Footer footer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer);

		Intent i = getIntent();
		entity = (NotificationEntity) i.getSerializableExtra("entity");
		searchTerm = entity.notificationData;

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
		footer.onDestroy();
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
		footer = (Footer) findViewById(R.id.footer);
		footer.init(entity);
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
			String apiKey = getResources().getString(R.string.yahoo_news_key);
			loadNewsTask = (LoadYahooNewsTask) new LoadYahooNewsTask(this).execute(searchTerm,
					apiKey);
		} else
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
			((TextView) view.findViewById(R.id.news_item_when_where)).setText(item.timestamp
					.format("%b %d")
					+ ", " + item.source);

			return view;
		}

	}

	static class NewsListHolder {
		ArrayList<NewsItem> items;
		LoadYahooNewsTask loadNewsTask;
	}

	public void onLoadYahooNews(ArrayList<NewsItem> news) {
		this.items = news;
		displayNews();
		loadNewsTask = null;
	}
}