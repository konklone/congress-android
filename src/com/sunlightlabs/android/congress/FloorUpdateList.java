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
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscription;
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
	
	private Footer footer;
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.list_footer);
		
		Bundle extras = getIntent().getExtras();
		this.chamber = extras.getString("chamber");
		
		FloorUpdateListHolder holder = (FloorUpdateListHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			updates = holder.updates;
			loadUpdatesTask = holder.loadUpdatesTask;
			tracked = holder.tracked;
			footer = holder.footer;
		}
		
		tracker = Analytics.start(this);
		if (!tracked) {
			Analytics.page(this, tracker, "/floor_updates/" + chamber);
			tracked = true;
		}
		
		if (footer != null)
			footer.onScreenLoad(this, tracker);
		else
			footer = Footer.from(this, tracker);
		
		if (loadUpdatesTask == null)
			loadUpdates();
		else
			loadUpdatesTask.onScreenLoad(this);
		
		setupControls();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new FloorUpdateListHolder(updates, loadUpdatesTask, footer, tracked);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Analytics.stop(tracker);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (updates != null)
			setupSubscription();
	}
	
	private void setupControls() {
		((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});

		Utils.setLoading(this, R.string.floor_updates_loading);
	}
	
	private void setupSubscription() {
		footer.init(new Subscription(chamber, chamber, "FloorUpdatesSubscriber", chamber), updates);
	}
	
	private void refresh() {
		updates = null;
		Utils.showLoading(this);
		loadUpdates();
	}
	
	private void selectRoll(String rollId) {
		startActivity(Utils.rollIntent(this, rollId));
	}
	
	private void selectBill(String billId) {
		startActivity(Utils.billLoadIntent(billId));
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
		if (updates.size() > 0) {
			setListAdapter(new FloorUpdateAdapter(this, FloorUpdateAdapter.wrapUpdates(updates)));
			setupSubscription();
		} else
			Utils.showRefresh(this, R.string.floor_updates_error); // should not happen
	}
	
	static class FloorUpdateAdapter extends ArrayAdapter<FloorUpdateAdapter.UpdateWrapper> {
    	LayoutInflater inflater;
    	Resources resources;
    	
    	static SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd");
    	static SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm aa");
    	
    	public static final int TYPE_DATE = 0;
    	public static final int TYPE_UPDATE = 1;

        public FloorUpdateAdapter(Activity context, List<FloorUpdateAdapter.UpdateWrapper> items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
            resources = context.getResources();
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
        public int getViewTypeCount() {
        	return 1;
        }

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			UpdateWrapper item = getItem(position);
			FloorUpdate update = item.update;
			
			view = inflater.inflate(R.layout.floor_update, null);
			view.setEnabled(false);
			
			ViewGroup dateView = (ViewGroup) view.findViewById(R.id.date_line);
			TextView timeView = (TextView) view.findViewById(R.id.timestamp);
			
			Calendar calendar = GregorianCalendar.getInstance();
			String today = dateFormat.format(calendar.getTime());
			String date = dateFormat.format(update.timestamp);
			if (today.equals(date))
				date = "Today, " + date;
			
			((TextView) view.findViewById(R.id.datestamp)).setText(date);
			
			timeView.setText(timeFormat.format(update.timestamp));
			if (update.events.size() > 0)
				((TextView) view.findViewById(R.id.text)).setText(update.events.get(0));

			if (item.showDate)
				dateView.setVisibility(View.VISIBLE);
			else
				dateView.setVisibility(View.GONE);
			
			if (item.showTime)
				timeView.setVisibility(View.VISIBLE);
			else
				timeView.setVisibility(View.INVISIBLE);
			
			return view;
		}
		
		static class UpdateWrapper {
			FloorUpdate update;
			boolean showDate, showTime;
			
			public UpdateWrapper(FloorUpdate update) {
				this.update = update;
				this.showDate = false;
				this.showTime = false;
			}
		}
		
		static List<UpdateWrapper> wrapUpdates(List<FloorUpdate> updates) {
			List<UpdateWrapper> wrappers = new ArrayList<UpdateWrapper>();
			
			String currentDate = "";
			String currentTime = "";
			
			for (int i=0; i<updates.size(); i++) {
				FloorUpdate update = updates.get(i);
				UpdateWrapper wrapper = new UpdateWrapper(update);
				
				String date = dateFormat.format(update.timestamp);
				String time = timeFormat.format(update.timestamp);
				
				if (!currentDate.equals(date))
					wrapper.showDate = true;
				
				if (!currentTime.equals(time))
					wrapper.showTime = true;
				
				currentDate = date;
				currentTime = time;
				
				wrappers.add(wrapper);
			}
			
			return wrappers;
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
		Footer footer;
		boolean tracked;
		
		FloorUpdateListHolder(List<FloorUpdate> updates, LoadUpdatesTask loadUpdatesTask, Footer footer, boolean tracked) {
			this.updates = updates;
			this.loadUpdatesTask = loadUpdatesTask;
			this.footer = footer;
			this.tracked = tracked;
		}
	}
}