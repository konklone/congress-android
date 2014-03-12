package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadNewsTask;
import com.sunlightlabs.android.congress.tasks.LoadNewsTask.LoadsNews;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.google.news.NewsItem;

public class NewsListFragment extends ListFragment implements LoadsNews {
	private String searchTerm;
	
	private List<NewsItem> items;
	private List<String> newIds;
	
	private String subscriptionId, subscriptionName, subscriptionClass;

	public static NewsListFragment forBill(Bill bill) {
		NewsListFragment frag = new NewsListFragment();
		Bundle args = new Bundle();
		
		args.putString("searchTerm", Bill.searchTermFor(bill));
		args.putString("subscriptionId", bill.id);
		args.putString("subscriptionName", Subscriber.notificationName(bill));
		args.putString("subscriptionClass", "NewsBillSubscriber");
		
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static NewsListFragment forLegislator(Legislator legislator) {
		NewsListFragment frag = new NewsListFragment();
		Bundle args = new Bundle();
		
		args.putString("searchTerm", Legislator.searchTermFor(legislator));
		args.putString("subscriptionId", legislator.bioguide_id);
		args.putString("subscriptionName", Subscriber.notificationName(legislator));
		args.putString("subscriptionClass", "NewsLegislatorSubscriber");
		
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	
	public NewsListFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		searchTerm = args.getString("searchTerm");
		subscriptionId = args.getString("subscriptionId");
		subscriptionName = args.getString("subscriptionName");
		subscriptionClass = args.getString("subscriptionClass");
		
		loadNews();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_footer, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		newIds = FragmentUtils.newIds(this, subscriptionClass);
		
		setupControls();
		
		if (items != null) {
			displayNews();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (items != null) {
			setupSubscription();
		}
	}
	
	private void setupControls() {
		FragmentUtils.setLoading(this, R.string.news_loading);
		
		TextView header = (TextView) getView().findViewById(R.id.header_simple_text);
		header.setText(R.string.google_news_branding);
		header.setVisibility(View.VISIBLE);
		
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				items = null;
				FragmentUtils.showLoading(NewsListFragment.this);
				loadNews();
			}
		});
	}

	private void setupSubscription() {
		Footer.setup(this, new Subscription(subscriptionId, subscriptionName, subscriptionClass, searchTerm), items);
	}

	@Override
	public void onListItemClick(ListView parent, View view, int position, long id) {
		launchNews((NewsItem) parent.getItemAtPosition(position));
	}

	private void launchNews(NewsItem item) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.clickURL)));
	}

	protected void loadNews() {
		String apiKey = getResources().getString(R.string.google_news_key);
		String referer = getResources().getString(R.string.google_news_referer);
		new LoadNewsTask(this).execute(searchTerm, apiKey, referer);
	}
	
	@Override
	public void onLoadNews(List<NewsItem> items) {
		if (newIds != null) {
			Collections.sort(items, new Comparator<NewsItem>() {
				@Override
				public int compare(NewsItem a, NewsItem b) {
					// identical to decodeId method in the subscriber classes
					String aId = "" + a.timestamp.getTime();
					String bId = "" + b.timestamp.getTime();
					boolean hasA = newIds.contains(aId);
					boolean hasB = newIds.contains(bId);
					if (hasA && !hasB) {
						return -1;
					} else if (!hasA && hasB) {
						return 1;
					} else {
						return 0;
					}
				}
			});
		}
		
		this.items = items;
		if (isAdded()) {
			displayNews();
		}
	}

	@Override
	public void onLoadNews(CongressException e) {
		if (isAdded()) {
			FragmentUtils.showRefresh(this, R.string.news_empty);
		}		
	}

	protected void displayNews() {
		if (items != null && items.size() > 0) {
			setListAdapter(new NewsAdapter(this, items));
		} else {
			FragmentUtils.showRefresh(this, R.string.news_empty);
		}
		
		setupSubscription();
	}

	protected class NewsAdapter extends ArrayAdapter<NewsItem> {
		LayoutInflater inflater;

		public NewsAdapter(Fragment context, List<NewsItem> items) {
			super(context.getActivity(), 0, items);
			inflater = LayoutInflater.from(context.getActivity());
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
			if (view == null) {
				view = inflater.inflate(R.layout.news_item, null);
			}

			NewsItem item = getItem(position);
			
			TextView date = (TextView) view.findViewById(R.id.date);
			shortDate(date, item.timestamp);
			
			((TextView) view.findViewById(R.id.news_item_title)).setText(item.title);
			((TextView) view.findViewById(R.id.news_item_summary)).setText(Html.fromHtml(Utils.truncate(item.summary, 140)));
			((TextView) view.findViewById(R.id.news_where)).setText(item.source);
			
			String id = "" + item.timestamp.getTime();
			if (newIds != null && newIds.contains(id)) {
				view.findViewById(R.id.new_result).setVisibility(View.VISIBLE);
			} else {
				view.findViewById(R.id.new_result).setVisibility(View.GONE);
			}
			
			return view;
		}
		
		private void shortDate(TextView view, Date date) {
			if (date.getYear() == Calendar.getInstance().get(Calendar.YEAR)) { 
				view.setTextSize(18);
				view.setText(new SimpleDateFormat("MMM d", Locale.US).format(date).toUpperCase(Locale.US));
			} else {
				longDate(view, date);
			}
		}
		
		private void longDate(TextView view, Date date) {
			view.setTextSize(14);
			view.setText(new SimpleDateFormat("MMM d, ''yy").format(date).toUpperCase());
		}

	}
}