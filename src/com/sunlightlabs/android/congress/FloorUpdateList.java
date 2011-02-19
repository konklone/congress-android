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
import android.widget.ListView;
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
	
	@Override
	protected void onListItemClick(ListView parent, View v, int position, long id) {
		FloorUpdateAdapter.Item item = (FloorUpdateAdapter.Item) parent.getItemAtPosition(position);
		if (item instanceof FloorUpdateAdapter.Roll)
			selectRoll(((FloorUpdateAdapter.Roll) item).rollId);
	}

	private void selectRoll(String rollId) {
		startActivity(Utils.rollIntent(this, rollId));
	}
	
	public void loadUpdates() {
		if (updates == null)
			loadUpdatesTask = (LoadUpdatesTask) new LoadUpdatesTask(this).execute(chamber);
		else
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
			setListAdapter(new FloorUpdateAdapter(this, FloorUpdateAdapter.transformUpdates(updates)));
		else
			Utils.showRefresh(this, R.string.floor_updates_error); // should not happen
	}
	
	static class FloorUpdateAdapter extends ArrayAdapter<FloorUpdateAdapter.Item> {
    	LayoutInflater inflater;
    	Resources resources;
    	
    	public static final int TYPE_DATE = 0;
    	public static final int TYPE_UPDATE = 1;
    	public static final int TYPE_ROLL = 2;

        public FloorUpdateAdapter(Activity context, List<FloorUpdateAdapter.Item> items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
            resources = context.getResources();
        }
        
        @Override
        public boolean isEnabled(int position) {
        	if (getItemViewType(position) == FloorUpdateAdapter.TYPE_ROLL)
        		return true;
        	else
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
        	else if (item instanceof FloorUpdateAdapter.Update)
        		return FloorUpdateAdapter.TYPE_UPDATE;
        	else // roll
        		return FloorUpdateAdapter.TYPE_ROLL;
        }
        
        @Override
        public int getViewTypeCount() {
        	return 3;
        }

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Item item = getItem(position);
			if (item instanceof Date) {
				if (view == null)
					view = inflater.inflate(R.layout.floor_update_date, null);
				
				((TextView) view.findViewById(R.id.date)).setText(((Date) item).date);				
				
			} else if (item instanceof Update){
				// don't recycle
				view = inflater.inflate(R.layout.floor_update, null);
				
				SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm aa");
				
				FloorUpdate update = ((Update) item).update;
				((TextView) view.findViewById(R.id.timestamp)).setText(timeFormat.format(update.timestamp));
				
				// go in reverse order, so that the most recent thing is always at the top
				int length = update.events.size();
				for (int i=length-1; i>=0; i--) {
					View event = inflater.inflate(R.layout.floor_update_event, null);
					((TextView) event.findViewById(R.id.text)).setText(update.events.get(i));
					((ViewGroup) view).addView(event);
				}
			} else { // instanceof Roll
				view = inflater.inflate(R.layout.floor_update_roll, null);
				
				String rollId = ((Roll) item).rollId;
				((TextView) view.findViewById(R.id.roll)).setText(Utils.formatRollId(rollId));
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
		
		static class Roll extends Item {
			String rollId;
			
			public Roll(String rollId) {
				this.rollId = rollId;
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
				
				// 1) see if a date needs to be pre-prended
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
				
				// 2) add the update itself
				items.add(new Update(update));
				
				// 3) See if one or more roll call votes need to be appended
				int length = (update.rollIds != null ? update.rollIds.size() : 0);
				if (length > 0) {
					for (int j=0; j<length; j++)
						items.add(new Roll(update.rollIds.get(j)));
				}
					
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