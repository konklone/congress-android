package com.sunlightlabs.android.congress;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.models.Roll.Vote;
import com.sunlightlabs.congress.services.RollService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/*
 * Eric's notes - this class should be refactored into a RollPager activity,
 * and a RollInfoFragment. This would remove a lot of the screen-flipping logic
 * from this class, and bring it into line with the best practices elsewhere.
 * Cribbing from LegislatorProfileFragment and BillInfoFragment would make this easy.
 *
 * This would also make it easier to make new tabs, such as showing a breakdown
 * of the votes by party, etc. *
 */
public class RollInfo extends ListActivity implements LoadPhotoTask.LoadsPhoto {
	private String id;
	
	private Roll roll;
	
	private Database database;
	private Cursor peopleCursor;
	
	private LoadRollTask loadRollTask;
	private View header, loadingView;
	
	private Map<String,LoadPhotoTask> loadPhotoTasks = new HashMap<String,LoadPhotoTask>();
	private List<String> queuedPhotos = new ArrayList<>();
	
	private static final int MAX_PHOTO_TASKS = 10;
	private static final int MAX_QUEUE_TASKS = 20;
	
	// keep the adapters and arrays as members so we can toggle freely between them
	private List<Roll.Vote> starred = new ArrayList<>();
	private List<Roll.Vote> rest = new ArrayList<>();

	private String currentTab = null;
	private Map<String, List<Roll.Vote>> voterBreakdown = new HashMap<>();
	
	LayoutInflater inflater;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Analytics.init(this);
		setContentView(R.layout.list_titled_fastscroll);
		
		inflater = LayoutInflater.from(this);
		
		database = new Database(this);
		database.open();
		peopleCursor = database.getLegislators();
		startManagingCursor(peopleCursor);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		roll = (Roll) extras.getSerializable("roll");

        // coming in from hyperlinked floor update?
		Intent intent = getIntent();
		if (intent != null) {
			Uri uri = intent.getData();
			if (uri != null) {
				List<String> segments = uri.getPathSegments();
				if (segments.size() == 4) {
					String chamber = segments.get(1);
					String year = segments.get(2);
					String formattedNumber = segments.get(3);
					id = Roll.normalizeRollId(chamber, year, formattedNumber);
				}
			}
		}
		
		setupControls();
		
		RollInfoHolder holder = (RollInfoHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.loadRollTask = holder.loadRollTask;
			this.roll = holder.roll;
			this.loadPhotoTasks = holder.loadPhotoTasks;
			this.currentTab = holder.currentTab;
			
			if (loadPhotoTasks != null) {
				for (LoadPhotoTask loadPhotoTask : loadPhotoTasks.values())
					loadPhotoTask.onScreenLoad(this);
			}
		}
		loadRoll();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new RollInfoHolder(loadRollTask, roll, loadPhotoTasks, currentTab);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Analytics.start(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Analytics.stop(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, Utils.formatRollId(id), new Intent(this, MenuVotes.class));
		Utils.setLoading(this, R.string.vote_loading);
	}
	
	@Override
	public void onListItemClick(ListView parent, View view, int position, long id) {
		Object tag = view.getTag();
		if (tag != null) {
			if (tag instanceof VoterAdapter.ViewHolder)
				startActivity(Utils.legislatorIntent(((VoterAdapter.ViewHolder) tag).bioguide_id));
		}
	}
	
	public void loadRoll() {
		if (loadRollTask != null)
			loadRollTask.onScreenLoad(this);
		else {
			if (roll != null)
				displayRoll();
			else
				loadRollTask = (LoadRollTask) new LoadRollTask(this, id).execute();
		}
	}

	public void onLoadRoll(Roll roll) {
        this.loadRollTask = null;
        this.roll = roll;
        displayRoll();
	}
	
	public void onLoadRoll(CongressException exception) {
        if (exception instanceof CongressException.NotFound)
            Utils.alert(this, R.string.vote_not_found);
        else
            Utils.alert(this, R.string.error_connection);

        this.loadRollTask = null;
        finish();
	}
	
	public void displayRoll() {
		LayoutInflater inflater = LayoutInflater.from(this);
		MergeAdapter adapter = new MergeAdapter();
		
		View headerTop = inflater.inflate(R.layout.roll_basic_1, null);
		
		((TextView) headerTop.findViewById(R.id.question)).setText(roll.question);
        ((TextView) headerTop.findViewById(R.id.description)).setText(roll.description);
		((TextView) headerTop.findViewById(R.id.voted_at)).setText(new SimpleDateFormat("MMM dd, yyyy")
				.format(roll.voted_at).toUpperCase());

		adapter.addView(headerTop);
		
		if (roll.bill_id != null && !roll.bill_id.equals("")) {
			View header = inflater.inflate(R.layout.header, null);
			TextView related = header.findViewById(R.id.header_text);
			related.setText(R.string.vote_related_to_bill);
			adapter.addView(header);
			
			
			View bill = inflater.inflate(R.layout.roll_bill, null);
			((TextView) bill.findViewById(R.id.code)).setText(Bill.formatCode(roll.bill_id));

			TextView titleView = bill.findViewById(R.id.bill_title);
			if (roll.bill_title != null) {
				titleView.setTextSize(14);
				titleView.setText(Utils.truncate(roll.bill_title, 200));
			} else {
				titleView.setTextSize(16);
				titleView.setText(R.string.bill_no_title_yet);
			}

			bill.setOnClickListener(v -> startActivity(Utils.billIntent(roll.bill_id)));
			
			adapter.addView(bill);
		}
		
		header = inflater.inflate(R.layout.roll_basic_2, null);
		
		View resultHeader = header.findViewById(R.id.result_header);
		((TextView) resultHeader.findViewById(R.id.header_text)).setText(R.string.vote_results_header);
		
		String requiredText = roll.required.equals("QUORUM") ? "Quorum" : roll.required + " majority required";
		((TextView) resultHeader.findViewById(R.id.header_text_right)).setText(requiredText);
		
		((TextView) header.findViewById(R.id.result)).setText(roll.result);
		
		loadingView = header.findViewById(R.id.loading_votes);
		((TextView) loadingView.findViewById(R.id.loading_message)).setText("Loading votes...");
		
		setupTabs();
		
		adapter.addView(header);
		setListAdapter(adapter);

		displayVoters();
	}
	
	// depends on the "header" member variable having been initialized and inflated
	public void setupTabs() {
		View.OnClickListener tabListener = view -> {
			String tag = (String) view.getTag();
			for (String tabTag : voterBreakdown.keySet()) {
				if (tabTag.equals(tag))
					header.findViewWithTag(tabTag).setSelected(true);
				else
					header.findViewWithTag(tabTag).setSelected(false);
			}

			currentTab = tag;
			toggleVoters(tag);
		};

		LinearLayout tabContainer = header.findViewById(R.id.vote_tabs);
		
		// yea and nay should always be first and second, if present
		// present and not voting should always be second to last and last
		Comparator<String> tabSorter = (one, other) -> {
			switch (one) {
				case Roll.NOT_VOTING:
					return 1;
				case Roll.PRESENT:
					if (other.equals(Roll.NOT_VOTING))
						return -1;
					else
						return 1;
				case Roll.YEA:
					return -1;
				case Roll.NAY:
					if (other.equals(Roll.YEA))
						return 1;
					else
						return -1;
				default:
					if (other.equals(Roll.NOT_VOTING) || other.equals(Roll.PRESENT))
						return -1;
					else
						return one.compareTo(other);
			}
		};
		
		Iterator<String> iter = roll.voteBreakdown.keySet().iterator();
		List<String> names = new ArrayList<>();
		while (iter.hasNext())
			names.add(iter.next());
		
		Collections.sort(names, tabSorter);
		
		for (int i=0; i<names.size(); i++) {
			String name = names.get(i);
			if (i == 0 && currentTab == null)
				currentTab = name;
			
			addTab(name, tabContainer, tabListener);
		}
	}
	
	public void addTab(String name, LinearLayout parent, View.OnClickListener tabListener) {
		View tab = inflater.inflate(R.layout.tab_2, null);
		
		String displayName;
		if (name.equals(Roll.NOT_VOTING))
			displayName = getResources().getString(R.string.not_voting_short);
		else
			displayName = name;
		
		((TextView) tab.findViewById(R.id.name)).setText(displayName);
		((TextView) tab.findViewById(R.id.subname)).setText(roll.voteBreakdown.get(name) + "");
		
		tab.setTag(name);
		tab.setOnClickListener(tabListener);
		
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		parent.addView(tab, params);

		voterBreakdown.put(name, new ArrayList<>());
	}
	
	
	// depends on setupTabs having been called, and that every vote a legislator has cast
	// has an entry in voterBreakdown, as created in setupTabs
	public void displayVoters() {
		if (roll.voters != null) {
			// sort Map of voters into the voterBreakdown Map by vote type
			List<Roll.Vote> allVoters = new ArrayList<Roll.Vote>(roll.voters.values());
			Collections.sort(allVoters); // sort once, all at once

			for (Vote vote : allVoters) {
				voterBreakdown.get(vote.vote).add(vote);
			}
			
			// hide loading, show tabs
			loadingView.setVisibility(View.GONE);
			
			header.findViewWithTag(currentTab).setSelected(true);
			header.findViewById(R.id.vote_tabs).setVisibility(View.VISIBLE);
			
			// initialize adapters, add them beneath the tabs
			VoterAdapter starredAdapter = new VoterAdapter(this, starred);
			VoterAdapter restAdapter = new VoterAdapter(this, rest);
			
			MergeAdapter adapter = (MergeAdapter) getListAdapter();
			adapter.addAdapter(starredAdapter);
			adapter.addAdapter(restAdapter);
			setListAdapter(adapter);
			
			// show the voters for the current tab
			toggleVoters(currentTab);
		} else {
			loadingView.findViewById(R.id.loading_spinner).setVisibility(View.GONE);
			((TextView) loadingView.findViewById(R.id.loading_message)).setText(R.string.vote_no_voters_yet);
		}
	}
	
	public void toggleVoters(String tag) {
		rest.clear();
		starred.clear();
		
		rest.addAll(voterBreakdown.get(tag));
		
		// reset starred, sweep through the new array again
		int starredCount = peopleCursor.getCount();
		
		if (starredCount > 0) {
			List<String> starredIds = new ArrayList<>(starredCount);
			
			peopleCursor.moveToFirst();
			do {
				starredIds.add(peopleCursor.getString(peopleCursor.getColumnIndex("bioguide_id")));
			} while(peopleCursor.moveToNext());
			
			Iterator<Roll.Vote> iter = rest.iterator();
			while (iter.hasNext()) {
				Roll.Vote vote = iter.next();
				if (starredIds.contains(vote.voter_id)) {
					iter.remove();
					starred.add(vote);
				}
			}
		}
		
		((MergeAdapter) getListAdapter()).notifyDataSetChanged();
	}
	
	public void loadPhoto(String bioguide_id) {
		if (!loadPhotoTasks.containsKey(bioguide_id)) {
			
			// if we have free space, fetch the photo
			if (loadPhotoTasks.size() <= MAX_PHOTO_TASKS) {
				try {
					loadPhotoTasks.put(bioguide_id, (LoadPhotoTask) new LoadPhotoTask(this,
							LegislatorImage.PIC_SMALL, bioguide_id).execute(bioguide_id));
				} catch(RejectedExecutionException e) {
					Log.e(Utils.TAG, "[RollInfo] RejectedExecutionException occurred while loading photo.", e);
					loadNoPhoto(bioguide_id);
				}
			}
			
			// otherwise, add it to the queue for later
			else {
				if (queuedPhotos.size() > MAX_QUEUE_TASKS)
					queuedPhotos.clear();
				
				if (!queuedPhotos.contains(bioguide_id))
					queuedPhotos.add(bioguide_id);
			}
		}
	}
	
	public void onLoadPhoto(Drawable photo, Object tag) {
		loadPhotoTasks.remove(tag);
		
		VoterAdapter.ViewHolder holder = new VoterAdapter.ViewHolder();
		holder.bioguide_id = (String) tag;
		
		View result = getListView().findViewWithTag(holder);
		if (result != null) {
			if (photo != null)
				((ImageView) result.findViewById(R.id.photo)).setImageDrawable(photo);
			else 
				((ImageView) result.findViewById(R.id.photo)).setImageResource(R.drawable.person);
		}
		
		// if there's any in the queue, send the next one
		if (!queuedPhotos.isEmpty())
			loadPhoto(queuedPhotos.remove(0));
	}
	
	public void loadNoPhoto(String bioguide_id) {
		VoterAdapter.ViewHolder holder = new VoterAdapter.ViewHolder();
		holder.bioguide_id = bioguide_id;
		
		View result = getListView().findViewWithTag(holder);
		if (result != null)
			((ImageView) result.findViewById(R.id.photo)).setImageResource(R.drawable.person);
	}
	
	public Context getContext() {
		return this;
	}
	
	private class LoadRollTask extends AsyncTask<Void,Void,Roll> {
		private RollInfo context;
		private CongressException exception;
		private String rollId;
		
		public LoadRollTask(RollInfo context, String rollId) {
			this.context = context;
			this.rollId = rollId;
			Utils.setupAPI(context);
		}
		
		public void onScreenLoad(RollInfo context) {
			this.context = context;
		}
		
		@Override
		public Roll doInBackground(Void... nothing) {
			try {
				return RollService.find(rollId);
			} catch (CongressException exception) {
				this.exception = exception;
				return null;
			}
		}
		
		@Override
		public void onPostExecute(Roll roll) {
			if (isCancelled()) return;
			
			// last check - if the database is closed, then onDestroy must have run, 
			// even if the task didn't get marked as cancelled for some reason
			if (context.database.closed) return;
			
			if (exception != null && roll == null)
				context.onLoadRoll(exception);
			else
				context.onLoadRoll(roll);
		}
	}
	
	private static class VoterAdapter extends ArrayAdapter<Roll.Vote> {
		LayoutInflater inflater;
		RollInfo context;
		
	    public VoterAdapter(RollInfo context, List<Vote> rest) {
	        super(context, 0, rest);
	        this.context = context;
	        this.inflater = LayoutInflater.from(context);
	    }
	    
	    public boolean areAllItemsEnabled() {
	    	return true;
	    }
        
        @Override
        public int getViewTypeCount() {
        	return 1;
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			ViewHolder holder;
			if (convertView == null) {
				view = inflater.inflate(R.layout.legislator_voter, null);
				
				holder = new ViewHolder();
				holder.name = view.findViewById(R.id.name);
				holder.vote = view.findViewById(R.id.vote);
				holder.photo = view.findViewById(R.id.photo);
				
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}
			
			Roll.Vote vote = getItem(position);
			Legislator legislator = vote.voter;
			
			// used as the hook to get the legislator image in place when it's loaded
			// and to link to the legislator's activity
			holder.bioguide_id = vote.voter_id;
			
			holder.name.setText(nameFor(legislator));
			
			TextView voteView = holder.vote;
			String value = vote.vote;
			switch (value) {
				case Roll.YEA:
					voteView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
					break;
				case Roll.NAY:
					voteView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
					break;
				case Roll.PRESENT:
					voteView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
					break;
				case Roll.NOT_VOTING:
					voteView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
					break;
				default:
					voteView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
					break;
			}
			
			voteView.setText(vote.vote);

			LegislatorImage.setImageView(vote.voter_id, LegislatorImage.PIC_SMALL,
					context.getContext(), holder.photo);

			return view;
		}
		
		public String nameFor(Legislator legislator) {
			if (legislator.party != null && legislator.state != null && legislator.last_name != null
					&& legislator.first_name != null) {
				String position = legislator.party + "-" + legislator.state;
				return legislator.last_name + ", " + legislator.first_name + " [" + position + "]";
			} else
				return "";
		}

		static class ViewHolder {
			TextView name, vote;
			ImageView photo;
			String bioguide_id;
			
			@Override
			public boolean equals(Object other) {
				return other != null && other instanceof ViewHolder && this.bioguide_id.equals(((ViewHolder) other).bioguide_id);
			}
		}
	}
	
	static class RollInfoHolder {
		LoadRollTask loadRollTask;
		Roll roll;
		Map<String,LoadPhotoTask> loadPhotoTasks;
		String currentTab;

		public RollInfoHolder(LoadRollTask loadRollTask, Roll roll, Map<String, LoadPhotoTask> loadPhotoTasks,
							  String currentTab) {
			this.loadRollTask = loadRollTask;
			this.roll = roll;
			this.loadPhotoTasks = loadPhotoTasks;
			this.currentTab = currentTab;
		}
	}
}