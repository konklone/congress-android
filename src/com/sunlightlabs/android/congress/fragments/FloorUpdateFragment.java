package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.DateAdapterHelper;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.PaginationListener;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.FloorUpdate;
import com.sunlightlabs.congress.services.FloorUpdateService;

public class FloorUpdateFragment extends ListFragment implements PaginationListener.Paginates, DateAdapterHelper.StickyHeader {
	
	public static final int PER_PAGE = 20;
	private String chamber;
	
	private List<FloorUpdate> updates;
	
	FloorUpdateAdapter adapterHelper;
	PaginationListener pager;
	View loadingView;
	
	public static FloorUpdateFragment forChamber(String chamber) {
		FloorUpdateFragment frag = new FloorUpdateFragment();
		Bundle args = new Bundle();
		args.putString("chamber", chamber);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public FloorUpdateFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		chamber = getArguments().getString("chamber");
		
		loadUpdates();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View main = inflater.inflate(R.layout.list_footer, container, false);
		
		return main;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (updates != null) {
			displayUpdates();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (updates != null) {
			setupSubscription();
		}
	}
	
	public void setupControls() {
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refresh();
			}
		});
		
		adapterHelper = new FloorUpdateAdapter(this);
		
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		
		loadingView = inflater.inflate(R.layout.loading_page, null);
		loadingView.setVisibility(View.GONE);
		getListView().addFooterView(loadingView);
		
		pager = new PaginationListener(this);
		adapterHelper.setOnScrollListener(pager);
		
		FragmentUtils.setLoading(this, R.string.floor_updates_loading);
	}
	
	private void refresh() {
		updates = null;
		FragmentUtils.setLoading(this, R.string.floor_updates_loading);
		FragmentUtils.showLoading(this);
		loadUpdates();
	}
	
	public void loadUpdates() {
		new LoadUpdatesTask(this, chamber, 1).execute();
	}
	
	@Override
	public void loadNextPage(int page) {
		adapterHelper.setOnScrollListener(null);
		loadingView.setVisibility(View.VISIBLE);
		new LoadUpdatesTask(this, chamber, page).execute();
	}
	
	public void onLoadUpdates(List<FloorUpdate> updates, int page) {
		if (!isAdded()) {
			return;
		}
		
		if (page == 1) {
			this.updates = updates;
			displayUpdates();
		} else {
			this.updates.addAll(updates);
			adapterHelper.notifyDataSetChanged();
			loadingView.setVisibility(View.GONE);
		}
		
		// only re-enable the pagination if we got a full page back
		if (updates.size() == PER_PAGE) {
			adapterHelper.setOnScrollListener(pager);
		}
	}
	
	public void onLoadUpdates(CongressException exception) {
		if (isAdded()) {
			FragmentUtils.showRefresh(this, R.string.floor_updates_error);
		}
	}
	
	public void displayUpdates() {
		if (updates.size() > 0) {
			setListAdapter(adapterHelper.adapterFor(updates));
			setupSubscription();
		}
		else {
			FragmentUtils.showRefresh(this, R.string.floor_updates_error); // should not happen
		}
	}
	
	private void setupSubscription() {
		Footer.setup(this, new Subscription(chamber, chamber, "FloorUpdatesSubscriber", chamber), updates);
	}
	
	static class FloorUpdateAdapter extends DateAdapterHelper<FloorUpdate> {
		static SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm aa");
		
        public FloorUpdateAdapter(ListFragment context) {
            super(context);
        }
        
        @Override
        public Date dateFor(FloorUpdate update) {
        	return update.timestamp;
        }
        
        @Override
        public void updateStickyHeader(Date date, View view, TextView left, TextView right) {
        	String time = timeFormat.format(date);
        	left.setText(Utils.nearbyOrFullDate(date));
    		right.setText(time);
        }
        
        @Override
		public View contentView(FloorUpdate update, boolean showTime) {
			
			View view = inflater.inflate(R.layout.floor_update, null);
			view.setEnabled(false);
			
			TextView timeView = (TextView) view.findViewById(R.id.timestamp);
			
			timeView.setText(timeFormat.format(update.timestamp));
			
			if (update.update != null) {
				TextView text = (TextView) view.findViewById(R.id.text);
				text.setText(update.update);
				
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.setTime(update.timestamp);
				int year = calendar.get(Calendar.YEAR);
				
				Linkify.addLinks(text, 
						Pattern.compile("(S\\.|H\\.)(\\s?J\\.|\\s?R\\.|\\s?Con\\.| ?)(\\s?Res\\.)*\\s?\\d+", Pattern.CASE_INSENSITIVE), 
						"congress://com.sunlightlabs.android.congress/bill/" + update.congress+ "/");
				
				Linkify.addLinks(text,
						Pattern.compile("Roll (?:no.|Call) (\\d+)"),
						"congress://com.sunlightlabs.android.congress/roll/" + update.chamber + "/" + year + "/");
			}
			
			if (showTime) {
				timeView.setVisibility(View.VISIBLE);
			} else {
				timeView.setVisibility(View.INVISIBLE);
			}
			
			return view;
		}

    }
	
	private static class LoadUpdatesTask extends AsyncTask<Void, Void, List<FloorUpdate>> {
		private FloorUpdateFragment context;
		private CongressException exception;
		int page;
		String chamber;

		public LoadUpdatesTask(FloorUpdateFragment context, String chamber, int page) {
			FragmentUtils.setupAPI(context);
			this.context = context;
			this.page = page;
			this.chamber = chamber;
		}

		@Override
		protected List<FloorUpdate> doInBackground(Void... nothing) {
			List<FloorUpdate> updates = new ArrayList<FloorUpdate>();
			
			try {
				updates = FloorUpdateService.latest(chamber, page, PER_PAGE);
			} catch (CongressException e) {
				Log.e(Utils.TAG, "Error while loading floor updates for " + chamber + ": " + e.getMessage());
				this.exception = e;
				return null;
			}
			
			return updates;
		}

		@Override
		protected void onPostExecute(List<FloorUpdate> updates) {
			if (updates == null && exception != null) {
				context.onLoadUpdates(exception);
			} else {
				context.onLoadUpdates(updates, page);
			}
		}

	}
}