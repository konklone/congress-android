package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
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
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.FloorUpdate;
import com.sunlightlabs.congress.services.FloorUpdateService;

public class FloorUpdateFragment extends ListFragment {
	
	public static final int PER_PAGE = 40;
	private String chamber;
	
	private List<FloorUpdate> updates;
	
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
		
		if (updates != null)
			displayUpdates();
	}
	
	public void setupControls() {
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});

		FragmentUtils.setLoading(this, R.string.floor_updates_loading);
	}
	
	private void refresh() {
		updates = null;
		FragmentUtils.setLoading(this, R.string.floor_updates_loading);
		FragmentUtils.showLoading(this);
		loadUpdates();
	}
	
	public void loadUpdates() {
		new LoadUpdatesTask(this).execute(chamber);
	}
	
	public void onLoadUpdates(List<FloorUpdate> updates) {
		this.updates = updates;
		if (isAdded())
			displayUpdates();
	}
	
	public void onLoadUpdates(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.floor_updates_error);
	}
	
	public void displayUpdates() {
		if (updates.size() > 0) {
			setListAdapter(new FloorUpdateAdapter(this).adapterFor(updates));
			setupSubscription();
		} else
			FragmentUtils.showRefresh(this, R.string.floor_updates_error); // should not happen
	}
	
	private void setupSubscription() {
		Footer.setup(this, new Subscription(chamber, chamber, "FloorUpdatesSubscriber", chamber), updates);
	}
	
	static class FloorUpdateAdapter extends DateAdapterHelper<FloorUpdate> {
		static SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm aa");
		
        public FloorUpdateAdapter(Fragment context) {
            super(context);
        }
        
        @Override
        public Date dateFor(FloorUpdate update) {
        	return update.timestamp;
        }
        
        @Override
		public View contentView(ContentWrapper wrapper) {
			FloorUpdate update = wrapper.content;
			
			View view = inflater.inflate(R.layout.floor_update, null);
			view.setEnabled(false);
			
			TextView timeView = (TextView) view.findViewById(R.id.timestamp);
			
			timeView.setText(timeFormat.format(update.timestamp));
			if (update.events.size() > 0)
				((TextView) view.findViewById(R.id.text)).setText(update.events.get(0));
			
//			if (item.showTime)
//				timeView.setVisibility(View.VISIBLE);
//			else
//				timeView.setVisibility(View.INVISIBLE);
			
			return view;
		}

    }
	
	private class LoadUpdatesTask extends AsyncTask<String, Void, List<FloorUpdate>> {
		private FloorUpdateFragment context;
		
		private CongressException exception;

		public LoadUpdatesTask(FloorUpdateFragment context) {
			FragmentUtils.setupRTC(context);
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
}