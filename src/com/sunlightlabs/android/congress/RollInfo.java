package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.models.Roll.Vote;
import com.sunlightlabs.congress.services.RollService;

public class RollInfo extends ListActivity implements LoadPhotoTask.LoadsPhoto {
	public final static String TAG = "CONGRESS";
	
	private String id;
	
	private Roll roll;
	private HashMap<String,Roll.Vote> voters;
	
	private Database database;
	private Cursor peopleCursor;
	
	private LoadRollTask loadRollTask, loadVotersTask;
	private View header, loadingView;
	
	private HashMap<String,LoadPhotoTask> loadPhotoTasks = new HashMap<String,LoadPhotoTask>();
	private List<String> queuedPhotos = new ArrayList<String>();
	
	private static final int MAX_PHOTO_TASKS = 10;
	private static final int MAX_QUEUE_TASKS = 20;
	
	// keep the adapters and arrays as members so we can toggle freely between them
	private List<Roll.Vote> starred = new ArrayList<Roll.Vote>();
	private List<Roll.Vote> rest = new ArrayList<Roll.Vote>();
	private VoterAdapter starredAdapter;
	private VoterAdapter restAdapter;
	
	private String currentTab = null;
	private HashMap<String,List<Roll.Vote>> voterBreakdown = new HashMap<String,List<Roll.Vote>>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled_fastscroll);
		
		database = new Database(this);
		database.open();
		peopleCursor = database.getLegislators();
		startManagingCursor(peopleCursor);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		roll = (Roll) extras.getSerializable("roll");
		
		setupControls();
		
		RollInfoHolder holder = (RollInfoHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.loadRollTask = holder.loadRollTask;
			this.roll = holder.roll;
			this.loadVotersTask = holder.loadVotersTask;
			this.voters = holder.voters;
			this.loadPhotoTasks = holder.loadPhotoTasks;
			this.currentTab = holder.currentTab;
			
			if (loadPhotoTasks != null) {
				Iterator<LoadPhotoTask> iterator = loadPhotoTasks.values().iterator();
				while (iterator.hasNext())
					iterator.next().onScreenLoad(this);
			}
		}
		
		loadRoll();
	}
	
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new RollInfoHolder(loadRollTask, roll, loadVotersTask, voters, loadPhotoTasks, currentTab);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}
	
	public void setupControls() {
		Roll tempRoll = Roll.splitRollId(id);
		String title = Utils.capitalize(tempRoll.chamber) + " Roll No. " + tempRoll.number;
		((TextView) findViewById(R.id.title_text)).setText(title);
		Utils.setLoading(this, R.string.roll_loading);
	}
	
	@Override
	public void onListItemClick(ListView parent, View view, int position, long id) {
		Object tag = view.getTag();
		if (tag != null) {
			if (tag instanceof VoterAdapter.ViewHolder)
				startActivity(Utils.legislatorLoadIntent(((VoterAdapter.ViewHolder) tag).bioguide_id));
			else if (tag instanceof String && ((String) tag).equals("bill_id"))
				startActivity(Utils.billLoadIntent(roll.bill_id));
		}
	}
	
	public void loadRoll() {
		if (loadRollTask != null)
			loadRollTask.onScreenLoad(this);
		else {
			if (roll != null)
				displayRoll();
			else
				loadRollTask = (LoadRollTask) new LoadRollTask(this, id, "basic").execute("basic");
		}
	}
	
	public void loadVotes() {
		if (loadVotersTask != null)
			loadVotersTask.onScreenLoad(this);
		else {
			if (voters != null)
				displayVoters();
			else
				loadVotersTask = (LoadRollTask) new LoadRollTask(this, id, "voters").execute("voters");
		}
	}
	
	public void onLoadRoll(String tag, Roll roll) {
		if (tag.equals("basic")) {
			this.loadRollTask = null;
			this.roll = roll;
			displayRoll();
		} else if (tag.equals("voters")) {
			this.loadVotersTask = null;
			this.voters = roll.voters;
			displayVoters();
		}
	}
	
	public void onLoadRoll(String tag, CongressException exception) {
		Utils.alert(this, R.string.error_connection);
		if (tag.equals("basic")) {
			this.loadRollTask = null;
			finish();
		} else if (tag.equals("voters")) {
			this.loadVotersTask = null;
			
			View loadingView = findViewById(R.id.loading_votes);
			loadingView.findViewById(R.id.loading_spinner).setVisibility(View.GONE);
			((TextView) loadingView.findViewById(R.id.loading_message)).setText("Error loading votes.");
		}
	}
	
	public void displayRoll() {
		LayoutInflater inflater = LayoutInflater.from(this);
		MergeAdapter adapter = new MergeAdapter();
		
		View headerTop = inflater.inflate(R.layout.roll_basic_1, null);
		
		((TextView) headerTop.findViewById(R.id.question)).setText(roll.question);
		((TextView) headerTop.findViewById(R.id.voted_at)).setText(new SimpleDateFormat("MMM dd, yyyy").format(roll.voted_at));
		
		adapter.addView(headerTop);
		
		
		if (roll.bill_id != null && !roll.bill_id.equals("")) {
			adapter.addView(inflater.inflate(R.layout.line, null));
			
			View bill = inflater.inflate(R.layout.roll_bill, null);
			((TextView) bill.findViewById(R.id.code)).setText(Bill.formatId(roll.bill_id));
			bill.setTag("bill_id");
			
			List<View> billArray = new ArrayList<View>(1);
			billArray.add(bill);
			adapter.addAdapter(new ViewArrayAdapter(this, billArray));
		}
		
		
		header = inflater.inflate(R.layout.roll_basic_2, null);
		
		View resultHeader = header.findViewById(R.id.result_header);
		((TextView) resultHeader.findViewById(R.id.header_text)).setText("Results");
		
		String requiredText = roll.required.equals("QUORUM") ? "Quorum" : roll.required + " majority required";
		((TextView) resultHeader.findViewById(R.id.header_text_right)).setText(requiredText);
		
		((TextView) header.findViewById(R.id.result)).setText(roll.result);
		
		loadingView = header.findViewById(R.id.loading_votes);
		((TextView) loadingView.findViewById(R.id.loading_message)).setText("Loading votes...");
		
		setupTabs();
		
		adapter.addView(header);
		setListAdapter(adapter);
		
		// kick off vote loading
		loadVotes();
	}
	
	// depends on the "header" member variable having been initialized and inflated
	public void setupTabs() {
		View.OnClickListener tabListener = new View.OnClickListener() {
			public void onClick(View view) {
				String tag = (String) view.getTag();
				Iterator<String> iter = voterBreakdown.keySet().iterator();
				while (iter.hasNext()) {
					String tabTag = iter.next();
					if (tabTag.equals(tag))
						header.findViewWithTag(tabTag).setSelected(true);
					else
						header.findViewWithTag(tabTag).setSelected(false);
				}
				
				currentTab = tag;
				toggleVoters(tag);
			}
		};
		
		View yeas = header.findViewById(R.id.yeas_header);
		View nays = header.findViewById(R.id.nays_header);
		if (roll.otherVotes.isEmpty()) {
			((TextView) yeas.findViewById(R.id.name)).setText(R.string.yeas);
			((TextView) yeas.findViewById(R.id.subname)).setText(roll.yeas + "");
			yeas.setTag("yeas");
			voterBreakdown.put("yeas", new ArrayList<Roll.Vote>());
			yeas.setOnClickListener(tabListener);
			
			if (currentTab == null)
				currentTab = "yeas";
			
			((TextView) nays.findViewById(R.id.name)).setText(R.string.nays);
			((TextView) nays.findViewById(R.id.subname)).setText(roll.nays + "");
			nays.setTag("nays");
			voterBreakdown.put("nays", new ArrayList<Roll.Vote>());
			nays.setOnClickListener(tabListener);
		} else {
			// if a roll call has non-standard votes, it's the House election of the Speaker - only known exception 
			Iterator<String> names = roll.otherVotes.keySet().iterator();
			String first = names.next();
			String second = names.next();
			
			((TextView) yeas.findViewById(R.id.name)).setText(first);
			((TextView) yeas.findViewById(R.id.subname)).setText(roll.otherVotes.get(first) + "");
			yeas.setTag(first);
			voterBreakdown.put(first, new ArrayList<Roll.Vote>());
			yeas.setOnClickListener(tabListener);
			
			if (currentTab == null)
				currentTab = first;
			
			((TextView) nays.findViewById(R.id.name)).setText(second);
			((TextView) nays.findViewById(R.id.subname)).setText(roll.otherVotes.get(second) + "");
			nays.setTag(second);
			voterBreakdown.put(second, new ArrayList<Roll.Vote>());
			nays.setOnClickListener(tabListener);
		}
		
		View present = header.findViewById(R.id.present_header);
		((TextView) present.findViewById(R.id.name)).setText(R.string.present);
		((TextView) present.findViewById(R.id.subname)).setText(roll.present + "");
		present.setTag("present");
		voterBreakdown.put("present", new ArrayList<Roll.Vote>());
		present.setOnClickListener(tabListener);
		
		View not_voting = header.findViewById(R.id.not_voting_header);
		((TextView) not_voting.findViewById(R.id.name)).setText(R.string.not_voting_short);
		((TextView) not_voting.findViewById(R.id.subname)).setText(roll.not_voting + "");
		not_voting.setTag("not_voting");
		voterBreakdown.put("not_voting", new ArrayList<Roll.Vote>());
		not_voting.setOnClickListener(tabListener);
	}
	
	
	// depends on setupTabs having been called, and that every vote a legislator has cast
	// has an entry in voterBreakdown, as created in setupTabs
	public void displayVoters() {
		// sort HashMap of voters into the voterBreakdown hashmap by vote type
		List<Roll.Vote> allVoters = new ArrayList<Roll.Vote>(voters.values());
		Collections.sort(allVoters); // sort once, all at once
		
		Iterator<Roll.Vote> iter = allVoters.iterator();
		while (iter.hasNext()) {
			Roll.Vote vote = iter.next();
			String name;
			if (vote.vote == Roll.YEA)
				name = "yeas";
			else if (vote.vote == Roll.NAY) 
				name = "nays";
			else if (vote.vote == Roll.PRESENT)
				name = "present";
			else if (vote.vote == Roll.NOT_VOTING)
				name = "not_voting";
			else // vote.vote == Roll.OTHER
				name = vote.vote_name;
			
			voterBreakdown.get(name).add(vote);
		}
		
		// hide loading, show tabs
		loadingView.setVisibility(View.GONE);
		
		header.findViewWithTag(currentTab).setSelected(true);
		header.findViewById(R.id.vote_tabs).setVisibility(View.VISIBLE);
		
		// initialize adapters, add them beneath the tabs
		starredAdapter = new VoterAdapter(this, starred, true);
		restAdapter = new VoterAdapter(this, rest);
		
		MergeAdapter adapter = (MergeAdapter) getListAdapter();
		adapter.addAdapter(starredAdapter);
		adapter.addAdapter(restAdapter);
		setListAdapter(adapter);
		
		// show the voters for the current tab
		toggleVoters(currentTab);
	}
	
	public void toggleVoters(String tag) {
		rest.clear();
		starred.clear();
		
		rest.addAll(voterBreakdown.get(tag));
		
		// reset starred, sweep through the new array again
		int starredCount = peopleCursor.getCount();
		
		if (starredCount > 0) {
			List<String> starredIds = new ArrayList<String>(starredCount);
			
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
					loadPhotoTasks.put(bioguide_id, (LoadPhotoTask) new LoadPhotoTask(this, LegislatorImage.PIC_MEDIUM, bioguide_id).execute(bioguide_id));
				} catch(RejectedExecutionException e) {
					Log.e(TAG, "[RollInfo] RejectedExecutionException occurred while loading photo.", e);
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
			else // don't know the gender from here, default to female (to balance out how the shortcut image defaults to male)
				((ImageView) result.findViewById(R.id.photo)).setImageResource(R.drawable.no_photo_female);
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
			((ImageView) result.findViewById(R.id.photo)).setImageResource(R.drawable.no_photo_female);
	}
	
	public Context getContext() {
		return this;
	}
	
	@Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    super.onCreateOptionsMenu(menu); 
	    getMenuInflater().inflate(R.menu.roll, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.shortcut:
			sendBroadcast(Utils.shortcutIntent(this, roll)
    				.setAction("com.android.launcher.action.INSTALL_SHORTCUT"));
    		break;
    	}
    	
    	return true;
	}
	
	private class LoadRollTask extends AsyncTask<String,Void,Roll> {
		private RollInfo context;
		private CongressException exception;
		private String rollId, tag;
		
		public LoadRollTask(RollInfo context, String rollId, String tag) {
			this.context = context;
			this.rollId = rollId;
			this.tag = tag;
			Utils.setupDrumbone(context);
		}
		
		public void onScreenLoad(RollInfo context) {
			this.context = context;
		}
		
		@Override
		public Roll doInBackground(String... sections) {
			try {
				return RollService.find(rollId, sections[0]);
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
				context.onLoadRoll(tag, exception);
			else
				context.onLoadRoll(tag, roll);
		}
	}
	
	private static class VoterAdapter extends ArrayAdapter<Roll.Vote> {
		LayoutInflater inflater;
		RollInfo context;
		Resources resources;
		
		private boolean starred;

	    public VoterAdapter(RollInfo context, List<Vote> rest) {
	        super(context, 0, rest);
	        this.context = context;
	        this.resources = context.getResources();
	        this.inflater = LayoutInflater.from(context);
	        this.starred = false;
	    }
	    
	    public VoterAdapter(RollInfo context, List<Vote> starred2, boolean starred) {
	        super(context, 0, starred2);
	        this.context = context;
	        this.resources = context.getResources();
	        this.inflater = LayoutInflater.from(context);
	        this.starred = starred;
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
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.position = (TextView) view.findViewById(R.id.position);
				holder.vote = (TextView) view.findViewById(R.id.vote);
				holder.photo = (ImageView) view.findViewById(R.id.photo);
				holder.star = (ImageView) view.findViewById(R.id.star);
				
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
			holder.position.setText(positionFor(legislator));
			
			holder.star.setVisibility(starred ? View.VISIBLE : View.GONE);
			
			TextView voteView = holder.vote;
			int value = vote.vote;
			switch (value) {
			case Roll.YEA:
				voteView.setText("Yea");
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				voteView.setTextColor(resources.getColor(R.color.yea));
				break;
			case Roll.NAY:
				voteView.setText("Nay");
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				voteView.setTextColor(resources.getColor(R.color.nay));
				break;
			case Roll.PRESENT:
				voteView.setText("Present");
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				voteView.setTextColor(resources.getColor(R.color.present));
				break;
			case Roll.NOT_VOTING:
				voteView.setText("Not Voting");
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				voteView.setTextColor(resources.getColor(R.color.not_voting));
				break;
			case Roll.OTHER:
			default:
				voteView.setText(vote.vote_name);
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				voteView.setTextColor(resources.getColor(android.R.color.white));
				break;
			}
			 
			BitmapDrawable photo = LegislatorImage.quickGetImage(LegislatorImage.PIC_MEDIUM, legislator.bioguide_id, context);
			if (photo != null)
				holder.photo.setImageDrawable(photo);
			else {
				holder.photo.setImageResource(R.drawable.loading_photo);
				context.loadPhoto(legislator.bioguide_id);
			}
			
			return view;
		}
		
		public String nameFor(Legislator legislator) {
			return legislator.last_name + ", " + legislator.firstName();
		}
		
		public String positionFor(Legislator legislator) {
			String district = legislator.district;
			String stateName = Utils.stateCodeToName(context, legislator.state);
			
			String position = "";
			if (district.equals("Senior Seat"))
				position = "Senior Senator from " + stateName;
			else if (district.equals("Junior Seat"))
				position = "Junior Senator from " + stateName;
			else if (district.equals("0")) {
				if (legislator.title.equals("Rep"))
					position = "Representative for " + stateName + " At-Large";
				else
					position = legislator.fullTitle() + " for " + stateName;
			} else
				position = "Representative for " + stateName + "-" + district;
			
			return "(" + legislator.party + ") " + position; 
		}
		
		static class ViewHolder {
			TextView name, position, vote;
			ImageView photo, star;
			String bioguide_id;
			
			@Override
			public boolean equals(Object other) {
				return other != null && other instanceof ViewHolder && this.bioguide_id.equals(((ViewHolder) other).bioguide_id);
			}
		}
		
	}
	
	static class RollInfoHolder {
		private LoadRollTask loadRollTask, loadVotersTask;
		private Roll roll;
		private HashMap<String,Roll.Vote> voters;
		HashMap<String,LoadPhotoTask> loadPhotoTasks;
		private String currentTab;
		
		public RollInfoHolder(LoadRollTask loadRollTask, Roll roll, LoadRollTask loadVotersTask, HashMap<String,Roll.Vote> voters, HashMap<String,LoadPhotoTask> loadPhotoTasks, String currentTab) {
			this.loadRollTask = loadRollTask;
			this.roll = roll;
			this.loadVotersTask = loadVotersTask;
			this.voters = voters;
			this.loadPhotoTasks = loadPhotoTasks;
			this.currentTab = currentTab;
		}
	}
}