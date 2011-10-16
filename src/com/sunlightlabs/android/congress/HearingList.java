package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Hearing;
import com.sunlightlabs.congress.services.HearingService;

public class HearingList extends ListActivity {
	public static final int PER_PAGE = 40;
	
	private String chamber;
	
	private List<Hearing> hearings;
	private LoadHearingsTask loadHearingsTask;
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer_titled);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null)
			chamber = extras.getString("chamber");
		
		HearingListHolder holder = (HearingListHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			hearings = holder.hearings;
			loadHearingsTask = holder.loadHearingsTask;
			tracked = holder.tracked;
		}
		
		tracker = Analytics.start(this);
		if (!tracked) {
			Analytics.page(this, tracker, "/hearings/upcoming");
			tracked = true;
		}
		
		if (loadHearingsTask == null)
			loadHearings();
		else
			loadHearingsTask.onScreenLoad(this);
		
		setupControls();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new HearingListHolder(hearings, loadHearingsTask, tracked);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Analytics.stop(tracker);
	}
	
	private void setupControls() {
		((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				hearings = null;
				Utils.showLoading(HearingList.this);
				loadHearings();
			}
		});

		Utils.setTitle(this, R.string.hearings_title, R.drawable.hearings);
		Utils.setTitleSize(this, 18);
		Utils.setLoading(this, R.string.hearings_loading);
	}
	
	@Override
	protected void onListItemClick(ListView parent, View v, int position, long id) {
		
	}
	
	public void loadHearings() {
		if (hearings == null)
			loadHearingsTask = (LoadHearingsTask) new LoadHearingsTask(this).execute();
		else
			displayHearings();
	}
	
	public void onLoadHearings(List<Hearing> hearings) {
		loadHearingsTask = null;
		this.hearings = hearings;
		displayHearings();
	}
	
	public void onLoadHearings(CongressException exception) {
		loadHearingsTask = null;
		Utils.showRefresh(this, R.string.hearings_error);
	}
	
	public void displayHearings() {
		if (hearings.size() > 0) {
			setListAdapter(new HearingAdapter(this, hearings));
//			ViewGroup header = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.list_header_simple, null);
//			
//			((TextView) header.findViewById(R.id.text)).setText(R.string.google_news_branding);
//			getListView().addHeaderView(header, null, false);
		} else
			Utils.showRefresh(this, R.string.hearings_empty);
	}
	
	static class HearingAdapter extends ArrayAdapter<Hearing> {
    	LayoutInflater inflater;
    	Resources resources;
    	
    	public static final int TYPE_DATE = 0;
    	public static final int TYPE_HEARING = 1;
    	public static final int TYPE_COMMITTEE = 2;

        public HearingAdapter(Activity context, List<Hearing> items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
            resources = context.getResources();
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
			Hearing hearing = getItem(position);
		
			if (view == null)
				view = inflater.inflate(R.layout.hearing, null);
			
			Date date = hearing.occursAt;
			String month = new SimpleDateFormat("MMM d").format(date);
			String time = new SimpleDateFormat("h:mm aa").format(date);
			
			((TextView) view.findViewById(R.id.month)).setText(month);
			((TextView) view.findViewById(R.id.time)).setText(time);
			((TextView) view.findViewById(R.id.name)).setText(hearing.committee.name);
			((TextView) view.findViewById(R.id.room)).setText(hearing.room);
			((TextView) view.findViewById(R.id.description)).setText(Utils.truncate(hearing.description, 200));
			
			return view;
		}
    }
	
	private class LoadHearingsTask extends AsyncTask<String, Void, List<Hearing>> {
		private HearingList context;
		
		private CongressException exception;

		public LoadHearingsTask(HearingList context) {
			Utils.setupRTC(context);
			this.context = context;
		}

		public void onScreenLoad(HearingList context) {
			this.context = context;
		}

		@Override
		protected List<Hearing> doInBackground(String... params) {
			List<Hearing> hearings = new ArrayList<Hearing>();
			
			try {
				hearings = HearingService.upcoming(chamber, 1, PER_PAGE);
			} catch (CongressException e) {
				Log.e(Utils.TAG, "Error while loading committee hearings for " + chamber + ": " + e.getMessage());
				this.exception = e;
				return null;
			}
			
			return hearings;
		}

		@Override
		protected void onPostExecute(List<Hearing> hearings) {
			if (hearings == null && exception != null)
				context.onLoadHearings(exception);
			else
				context.onLoadHearings(hearings);
		}

	}
	
	private class HearingListHolder {
		List<Hearing> hearings;
		LoadHearingsTask loadHearingsTask;
		boolean tracked;
		
		HearingListHolder(List<Hearing> hearings, LoadHearingsTask loadHearingsTask, boolean tracked) {
			this.hearings = hearings;
			this.loadHearingsTask = loadHearingsTask;
			this.tracked = tracked;
		}
	}
}