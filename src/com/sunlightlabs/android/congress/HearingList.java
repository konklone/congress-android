package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
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
import com.sunlightlabs.android.congress.HearingList.HearingAdapter.CommitteeItem;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Committee;
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
		Utils.setLoading(this, R.string.hearings_loading);
	}
	
	@Override
	protected void onListItemClick(ListView parent, View v, int position, long id) {
		HearingAdapter.Item item = (HearingAdapter.Item) parent.getItemAtPosition(position);
		if (item instanceof HearingAdapter.CommitteeItem)
			selectCommittee(((CommitteeItem) item).committee);
	}

	private void selectCommittee(Committee committee) {
		startActivity(new Intent(this, LegislatorList.class)
			.putExtra("committeeId", committee.id)
			.putExtra("committeeName", committee.name)
			.putExtra("type", LegislatorList.SEARCH_COMMITTEE)
		);
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
		if (hearings.size() > 0)
			setListAdapter(new HearingAdapter(this, HearingAdapter.transformHearings(hearings)));
		else
			Utils.showRefresh(this, R.string.hearings_empty);
	}
	
	static class HearingAdapter extends ArrayAdapter<HearingAdapter.Item> {
    	LayoutInflater inflater;
    	Resources resources;
    	
    	public static final int TYPE_DATE = 0;
    	public static final int TYPE_HEARING = 1;
    	public static final int TYPE_COMMITTEE = 2;

        public HearingAdapter(Activity context, List<HearingAdapter.Item> items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
            resources = context.getResources();
        }
        
        @Override
        public boolean isEnabled(int position) {
        	return (getItemViewType(position) == HearingAdapter.TYPE_COMMITTEE);
        }
        
        @Override
        public boolean areAllItemsEnabled() {
        	return false;
        }
        
        @Override
        public int getItemViewType(int position) {
        	Item item = getItem(position);
        	if (item instanceof DateItem)
        		return HearingAdapter.TYPE_DATE;
        	else if (item instanceof HearingItem)
        		return HearingAdapter.TYPE_HEARING;
        	else // roll
        		return HearingAdapter.TYPE_COMMITTEE;
        }
        
        @Override
        public int getViewTypeCount() {
        	return 3;
        }

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Item item = getItem(position);
			if (item instanceof DateItem) {
				if (view == null)
					view = inflater.inflate(R.layout.header_date, null);
				
				((TextView) view.findViewById(R.id.date)).setText(((DateItem) item).date);				
				
			} else if (item instanceof HearingItem) {
				if (view == null)
					view = inflater.inflate(R.layout.hearing, null);
				
				SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d 'at' h:mm aa");
				
				Hearing hearing = ((HearingItem) item).hearing;
				String time = timeFormat.format(hearing.occursAt);
				
				String timeAndRoom = time + "";
				if (hearing.room != null)
					timeAndRoom += ", Room " + hearing.room;
				
				((TextView) view.findViewById(R.id.name)).setText(hearing.committee.name);
				((TextView) view.findViewById(R.id.time_and_room)).setText(timeAndRoom);
				((TextView) view.findViewById(R.id.description)).setText(hearing.description);
				
			} else { // instanceof CommitteeItem
				if (view == null)
					view = inflater.inflate(R.layout.hearing_committee, null);
				
				Committee committee = ((CommitteeItem) item).committee;
				((TextView) view.findViewById(R.id.text)).setText(committee.name);
				view.setTag(committee.id);
			}

			return view;
		}
		
		static class Item {}
		
		static class DateItem extends Item {
			String date;
			
			public DateItem(String date) {
				this.date = date;
			}
		}
		
		static class HearingItem extends Item {
			Hearing hearing;
			
			public HearingItem(Hearing hearing) {
				this.hearing = hearing;
			}
		}
		
		static class CommitteeItem extends Item {
			Committee committee;
			
			public CommitteeItem(Committee committee) {
				this.committee = committee;
			}
		}
		
		static List<Item> transformHearings(List<Hearing> hearings) {
			List<Item> items = new ArrayList<Item>();
			
//			SimpleDateFormat thisYearFormat = new SimpleDateFormat("MMMMMM dd, yyyy");
//			
//			int currentMonth = -1;
//			int currentDay = -1;
//			int currentYear = -1;
//			
			int length = hearings.size();
			for (int i=0; i<length; i++) {
				Hearing hearing = hearings.get(i);
//				
//				// 1) see if a date needs to be pre-prended
//				GregorianCalendar calendar = new GregorianCalendar(DateUtils.GMT);
//				calendar.setTime(hearing.occursAt);
//				
//				int month = calendar.get(Calendar.MONTH);
//				int day = calendar.get(Calendar.DAY_OF_MONTH);
//				int year = calendar.get(Calendar.YEAR);
//				
//				if (currentMonth != month || currentDay != day || currentYear != year) {
//					String timestamp = thisYearFormat.format(hearing.occursAt);
//					items.add(new DateItem(timestamp));
//					
//					currentMonth = month;
//					currentDay = day;
//					currentYear = year;
//				}
				
				// 2) add the hearing itself
				items.add(new HearingItem(hearing));
				
				// 3) append an item for an associated committee
				// items.add(new CommitteeItem(hearing.committee));
					
			}
			
			return items;
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
				hearings = HearingService.upcoming(1, PER_PAGE);
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