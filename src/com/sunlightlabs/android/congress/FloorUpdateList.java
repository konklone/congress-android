package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.FloorUpdate;
import com.sunlightlabs.congress.services.FloorUpdateService;

public class FloorUpdateList extends ListActivity {
	public static final int PER_PAGE = 40;
	
	private String chamber;
	
	private List<FloorUpdate> updates;
	private LoadUpdatesTask loadUpdatesTask;
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer_titled);
		
		Bundle extras = getIntent().getExtras();
		this.chamber = extras.getString("chamber");
		
		FloorUpdateListHolder holder = (FloorUpdateListHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			updates = holder.updates;
			loadUpdatesTask = holder.loadUpdatesTask;
			tracked = holder.tracked;
		}
		
		tracker = Analytics.start(this);
		if (!tracked) {
			Analytics.page(this, tracker, "/floor_updates/" + chamber);
			tracked = true;
		}
		
		if (loadUpdatesTask == null)
			loadUpdates();
		else
			loadUpdatesTask.onScreenLoad(this);
		
		setupControls();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new FloorUpdateListHolder(updates, loadUpdatesTask, tracked);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Analytics.stop(tracker);
	}
	
	private void setupControls() {
		((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				updates = null;
				Utils.showLoading(FloorUpdateList.this);
				loadUpdates();
			}
		});

		Utils.setTitle(this, R.string.menu_main_floor_updates, R.drawable.people);
		Utils.setLoading(this, R.string.floor_updates_loading);
	}
	
	public void loadUpdates() {
		if (updates == null) {
			loadUpdatesTask = (LoadUpdatesTask) new LoadUpdatesTask(this).execute(chamber);
		} else
			displayUpdates();
	}
	
	public void onLoadUpdates(List<FloorUpdate> updates) {
		loadUpdatesTask = null;
		this.updates = updates;
		displayUpdates();
	}
	
	public void onLoadUpdates(CongressException exception) {
		loadUpdatesTask = null;
		Utils.showRefresh(this, R.string.floor_updates_error);
	}
	
	public void displayUpdates() {
		if (updates.size() > 0)
			setListAdapter(new FloorUpdateAdapter(this, FloorUpdateAdapter.transformUpdates(updates), chamber));
		else
			Utils.showRefresh(this, R.string.floor_updates_error); // should not happen
	}
	
	static class FloorUpdateAdapter extends ArrayAdapter<FloorUpdateAdapter.Item> {
    	LayoutInflater inflater;
    	Resources resources;
    	String chamber;
    	
    	public static final int TYPE_DATE = 0;
    	public static final int TYPE_UPDATE = 1; 

        public FloorUpdateAdapter(Activity context, List<FloorUpdateAdapter.Item> items, String chamber) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
            resources = context.getResources();
            this.chamber = chamber; 
        }
        
        @Override
        public boolean isEnabled(int position) {
        	return false;
        }
        
        @Override
        public boolean areAllItemsEnabled() {
        	return false;
        }
        
        @Override
        public int getItemViewType(int position) {
        	Item item = getItem(position);
        	if (item instanceof FloorUpdateAdapter.Date)
        		return FloorUpdateAdapter.TYPE_DATE;
        	else
        		return FloorUpdateAdapter.TYPE_UPDATE;
        }
        
        @Override
        public int getViewTypeCount() {
        	return 2;
        }

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Item item = getItem(position);
			if (item instanceof Date) {
				if (view == null)
					view = inflater.inflate(R.layout.floor_update_date, null);
				
				((TextView) view.findViewById(R.id.date)).setText(((Date) item).date);				
				
			} else { // instanceof Update
				// don't recycle
				view = inflater.inflate(R.layout.floor_update, null);
				
				SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm aa");
				
				FloorUpdate update = ((Update) item).update;
				((TextView) view.findViewById(R.id.timestamp)).setText(timeFormat.format(update.timestamp));
				
				int length = update.events.size();
				for (int i=0; i<length; i++) {
					View event = inflater.inflate(R.layout.floor_update_event, null);
					((TextView) event.findViewById(R.id.text)).setText(update.events.get(i));
					((ViewGroup) view).addView(event);
				}
			}

			return view;
		}
		
		static class Item {}
		
		static class Date extends Item {
			String date;
			
			public Date(String date) {
				this.date = date;
			}
		}
		
		static class Update extends Item {
			FloorUpdate update;
			
			public Update(FloorUpdate update) {
				this.update = update;
			}
		}
		
		static List<Item> transformUpdates(List<FloorUpdate> updates) {
			List<Item> items = new ArrayList<Item>();
			
			SimpleDateFormat thisYearFormat = new SimpleDateFormat("MMMMMM dd, yyyy");
			
			int currentMonth = -1;
			int currentDay = -1;
			int currentYear = -1;
			
			for (int i=0; i<updates.size(); i++) {
				FloorUpdate update = updates.get(i);
				
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.setTime(update.timestamp);
				
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				int year = calendar.get(Calendar.YEAR);
				
				if (currentMonth != month || currentDay != day || currentYear != year) {
					String timestamp = thisYearFormat.format(update.timestamp);
					items.add(new Date(timestamp));
					
					currentMonth = month;
					currentDay = day;
					currentYear = year;
				}
				
				items.add(new Update(update));
			}
			
			return items;
		}

    }
	
	private class LoadUpdatesTask extends AsyncTask<String, Void, List<FloorUpdate>> {
		private FloorUpdateList context;
		
		private CongressException exception;

		public LoadUpdatesTask(FloorUpdateList context) {
			Utils.setupRTC(context);
			this.context = context;
		}

		public void onScreenLoad(FloorUpdateList context) {
			this.context = context;
		}

		@Override
		protected List<FloorUpdate> doInBackground(String... params) {
			List<FloorUpdate> updates = new ArrayList<FloorUpdate>();
			String chamber = params[0];
			
			try {
				updates = FloorUpdateService.latest(chamber, 1, PER_PAGE);
			} catch (CongressException e) {
				Log.e(Utils.TAG, "Error while loading floor updates for " + chamber + ": " + e.getMessage());
				this.exception = e;
				return null;
			}
			
			return updates;
		}

		@Override
		protected void onPostExecute(List<FloorUpdate> updates) {
			if (updates == null && exception != null)
				context.onLoadUpdates(exception);
			else
				context.onLoadUpdates(updates);
		}

	}
	
	private class FloorUpdateListHolder {
		List<FloorUpdate> updates;
		LoadUpdatesTask loadUpdatesTask;
		boolean tracked;
		
		FloorUpdateListHolder(List<FloorUpdate> updates, LoadUpdatesTask loadUpdatesTask, boolean tracked) {
			this.updates = updates;
			this.loadUpdatesTask = loadUpdatesTask;
			this.tracked = tracked;
		}
	}
}