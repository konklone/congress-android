package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Hearing;
import com.sunlightlabs.congress.services.HearingService;

public class HearingListFragment extends ListFragment {
	public static final int PER_PAGE = 40;
	
	private String chamber;
	
	private List<Hearing> hearings;
	
	public static HearingListFragment forChamber(String chamber) {
		HearingListFragment frag = new HearingListFragment();
		Bundle args = new Bundle();
		args.putString("chamber", chamber);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public HearingListFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		chamber = args.getString("chamber");
		
		loadHearings();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (hearings != null)
			displayHearings();
	}
	
	private void setupControls() {
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});

		FragmentUtils.setLoading(this, R.string.hearings_loading);
	}
	
	private void loadHearings() {
		new LoadHearingsTask(this).execute(chamber);
	}
	
	private void refresh() {
		hearings = null;
		FragmentUtils.setLoading(this, R.string.hearings_loading);
		FragmentUtils.showLoading(this);
		loadHearings();
	}
	
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		selectHearing((Hearing) parent.getItemAtPosition(position));
	}
	
	public void selectHearing(Hearing hearing) {
		Date date = hearing.occursAt;
		long startTime = date.getTime();
		long endTime = startTime + (3 * 60 * 60 * 1000);
		
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event");
		
		intent.putExtra("title", "Hearing: " + hearing.committee.name);
		intent.putExtra("description", hearing.description);
		intent.putExtra("eventLocation", hearing.room);
		
		intent.putExtra("beginTime", startTime);
		intent.putExtra("endTime", endTime);
		intent.putExtra("allDay", false);
		
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Utils.alert(getActivity(), R.string.hearings_no_calendar);
		}
	}
	
	public void onLoadHearings(List<Hearing> hearings) {
		this.hearings = hearings;
		displayHearings();
	}
	
	public void onLoadHearings(CongressException exception) {
		FragmentUtils.showRefresh(this, R.string.hearings_error);
	}
	
	public void displayHearings() {
		if (hearings.size() > 0) {
			TextView header = (TextView) getView().findViewById(R.id.header_simple_text);
			header.setText(R.string.hearings_header);
			header.setVisibility(View.VISIBLE);
			
			setListAdapter(new HearingAdapter(this, hearings));
		} else
			FragmentUtils.showEmpty(this, R.string.hearings_empty);
	}
	
	static class HearingAdapter extends ArrayAdapter<Hearing> {
    	LayoutInflater inflater;
    	Resources resources;
    	
    	public static final int TYPE_DATE = 0;
    	public static final int TYPE_HEARING = 1;
    	public static final int TYPE_COMMITTEE = 2;

        public HearingAdapter(HearingListFragment context, List<Hearing> items) {
            super(context.getActivity(), 0, items);
            inflater = LayoutInflater.from(context.getActivity());
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
			String month = new SimpleDateFormat("MMM d").format(date).toUpperCase();
			String time = new SimpleDateFormat("h:mm aa").format(date);
			
			String name = hearing.committee.name;
			
			// strip chamber prefix off of name
			String chamberCap = hearing.chamber.substring(0, 1).toUpperCase() + hearing.chamber.substring(1);
			name = name.replaceFirst("^" + chamberCap + " ", "");
			
			String room = hearing.room;
			if (room.equals("TBA"))
				room = "Room TBA";
			
			((TextView) view.findViewById(R.id.month)).setText(month);
			((TextView) view.findViewById(R.id.time)).setText(time);
			((TextView) view.findViewById(R.id.name)).setText(name);
			((TextView) view.findViewById(R.id.room)).setText(room);
			((TextView) view.findViewById(R.id.description)).setText(Utils.truncate(hearing.description, 200));
			
			return view;
		}
    }
	
	private static class LoadHearingsTask extends AsyncTask<String, Void, List<Hearing>> {
		private HearingListFragment context;
		
		private CongressException exception;

		public LoadHearingsTask(HearingListFragment context) {
			FragmentUtils.setupCongress(context);
			this.context = context;
		}

		@Override
		protected List<Hearing> doInBackground(String... params) {
			List<Hearing> hearings = new ArrayList<Hearing>();
			String chamber = params[0];
			
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
			if (exception != null)
				context.onLoadHearings(exception);
			else
				context.onLoadHearings(hearings);
		}

	}
}