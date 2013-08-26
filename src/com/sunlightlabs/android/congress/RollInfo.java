package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
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

public class RollInfo extends ListActivity implements LoadPhotoTask.LoadsPhoto {
	private String id;
	
	private Roll roll;
	private Map<String,Roll.Vote> voters;
	
	private Database database;
	private Cursor peopleCursor;
	
	private LoadRollTask loadRollTask, loadVotersTask;
	private View header, loadingView;
	
	private Map<String,LoadPhotoTask> loadPhotoTasks = new HashMap<String,LoadPhotoTask>();
	private List<String> queuedPhotos = new ArrayList<String>();
	
	private static final int MAX_PHOTO_TASKS = 10;
	private static final int MAX_QUEUE_TASKS = 20;
	
	// keep the adapters and arrays as members so we can toggle freely between them
	private List<Roll.Vote> starred = new ArrayList<Roll.Vote>();
	private List<Roll.Vote> rest = new ArrayList<Roll.Vote>();
	private VoterAdapter starredAdapter;
	private VoterAdapter restAdapter;
	
	private String currentTab = null;
	private Map<String,List<Roll.Vote>> voterBreakdown = new HashMap<String,List<Roll.Vote>>();
	
	LayoutInflater inflater;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled_fastscroll);
		
		inflater = LayoutInflater.from(this);
		
		database = new Database(this);
		database.open();
		peopleCursor = database.getLegislators();
		startManagingCursor(peopleCursor);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		roll = (Roll) extras.getSerializable("roll");
		
		Intent intent = getIntent();
		if (intent != null) {
			Uri uri = intent.getData();
			if (uri != null) {
				List<String> segments = uri.getPathSegments();
				if (segments.size() == 4) { // coming in from floor
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
				loadRollTask = (LoadRollTask) new LoadRollTask(this, id, "basic").execute(RollService.basicFields);
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
		if (tag.equals("basic")) {
			if (exception instanceof CongressException.NotFound)
				Utils.alert(this, R.string.vote_not_found);
			else
				Utils.alert(this, R.string.error_connection);
			
			this.loadRollTask = null;
			finish();
		} else if (tag.equals("voters")) {
			this.loadVotersTask = null;
			
			View loadingView = findViewById(R.id.loading_votes);
			loadingView.findViewById(R.id.loading_spinner).setVisibility(View.GONE);
			((TextView) loadingView.findViewById(R.id.loading_message)).setText(R.string.votes_error);
		}
	}
	
	public void displayRoll() {
		LayoutInflater inflater = LayoutInflater.from(this);
		MergeAdapter adapter = new MergeAdapter();
		
		View headerTop = inflater.inflate(R.layout.roll_basic_1, null);
		
		((TextView) headerTop.findViewById(R.id.question)).setText(simpleQuestion(roll));
		((TextView) headerTop.findViewById(R.id.voted_at)).setText(new SimpleDateFormat("MMM dd, yyyy").format(roll.voted_at).toUpperCase());
		
		adapter.addView(headerTop);
		
		if (roll.amendmentPurpose != null && !roll.amendmentPurpose.equals("")) {
			View amendment = inflater.inflate(R.layout.roll_amendment_purpose, null);
			//((TextView) amendment.findViewById(R.id.header_text)).setText(R.string.vote_amendment_purpose);
			((TextView) amendment.findViewById(R.id.amendment_purpose)).setText(roll.amendmentPurpose);
			amendment.setEnabled(false);
			adapter.addView(amendment);
		}
		
		if (roll.bill_id != null && !roll.bill_id.equals("")) {
			View header = inflater.inflate(R.layout.header, null);
			TextView related = (TextView) header.findViewById(R.id.header_text);
			
			if (roll.vote_type != null) {
				if (roll.vote_type.equals("passage"))
					related.setText(R.string.vote_related_to_bill_passage);
				else if (roll.vote_type.equals("cloture"))
					related.setText(R.string.vote_related_to_bill_cloture);
				else
					related.setText(R.string.vote_related_to_bill);
			} else
				related.setText(R.string.vote_related_to_bill);
			adapter.addView(header);
			
			
			View bill = inflater.inflate(R.layout.roll_bill, null);
			((TextView) bill.findViewById(R.id.code)).setText(Bill.formatCode(roll.bill_id));
			
			TextView titleView = (TextView) bill.findViewById(R.id.bill_title);
			if (roll.bill != null) {
				if (roll.bill.short_title != null) {
					titleView.setTextSize(16);
					titleView.setText(Utils.truncate(roll.bill.short_title, 200));
				} else if (roll.bill.official_title != null) {
					titleView.setTextSize(14);
					titleView.setText(Utils.truncate(roll.bill.official_title, 200));
				} else {
					titleView.setTextSize(16);
					titleView.setText(R.string.bill_no_title);
				}
			} else {
				titleView.setTextSize(16);
				titleView.setText(R.string.bill_no_title_yet);
			}
			
			bill.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(Utils.billIntent(roll.bill_id));
				}
			});
			
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
		
		// kick off vote loading
		loadVotes();
	}
	
	// if the roll's about a bill, strip out the bill information from the question
	public String simpleQuestion(Roll roll) {
		if (roll.bill != null)
			return TextUtils.split(roll.question, " -- ")[0];
		else
			return roll.question;
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
		
		LinearLayout tabContainer = (LinearLayout) header.findViewById(R.id.vote_tabs);
		
		// yea and nay should always be first and second, if present
		// present and not voting should always be second to last and last
		Comparator<String> tabSorter = new Comparator<String>() {
			public int compare(String one, String other) {
				if (one.equals(Roll.NOT_VOTING))
					return 1;
				else if (one.equals(Roll.PRESENT)) {
					if (other.equals(Roll.NOT_VOTING))
						return -1;
					else
						return 1;
				} else if (one.equals(Roll.YEA))
					return -1;
				else if (one.equals(Roll.NAY)) {
					if (other.equals(Roll.YEA))
						return 1;
					else
						return -1;
				} else {
					if (other.equals(Roll.NOT_VOTING) || other.equals(Roll.PRESENT))
						return -1;
					else
						return one.compareTo(other);
				}
			}
		};
		
		Iterator<String> iter = roll.voteBreakdown.keySet().iterator();
		List<String> names = new ArrayList<String>();
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
		
		voterBreakdown.put(name, new ArrayList<Roll.Vote>());
	}
	
	
	// depends on setupTabs having been called, and that every vote a legislator has cast
	// has an entry in voterBreakdown, as created in setupTabs
	public void displayVoters() {
		if (voters != null) {
			// sort Map of voters into the voterBreakdown Map by vote type
			List<Roll.Vote> allVoters = new ArrayList<Roll.Vote>(voters.values());
			Collections.sort(allVoters); // sort once, all at once
			
			Iterator<Roll.Vote> iter = allVoters.iterator();
			while (iter.hasNext()) {
				Roll.Vote vote = iter.next();
				voterBreakdown.get(vote.vote).add(vote);
			}
			
			// hide loading, show tabs
			loadingView.setVisibility(View.GONE);
			
			header.findViewWithTag(currentTab).setSelected(true);
			header.findViewById(R.id.vote_tabs).setVisibility(View.VISIBLE);
			
			// initialize adapters, add them beneath the tabs
			starredAdapter = new VoterAdapter(this, starred);
			restAdapter = new VoterAdapter(this, rest);
			
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
	
	private class LoadRollTask extends AsyncTask<String,Void,Roll> {
		private RollInfo context;
		private CongressException exception;
		private String rollId, tag;
		
		public LoadRollTask(RollInfo context, String rollId, String tag) {
			this.context = context;
			this.rollId = rollId;
			this.tag = tag;
			Utils.setupAPI(context);
		}
		
		public void onScreenLoad(RollInfo context) {
			this.context = context;
		}
		
		@Override
		public Roll doInBackground(String... fields) {
			try {
				return RollService.find(rollId, fields);
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
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.vote = (TextView) view.findViewById(R.id.vote);
				holder.photo = (ImageView) view.findViewById(R.id.photo);
				
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
			if (value.equals(Roll.YEA))
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
			else if (value.equals(Roll.NAY))
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
			else if (value.equals(Roll.PRESENT))
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
			else if (value.equals(Roll.NOT_VOTING))
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
			else
				voteView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
			
			voteView.setText(vote.vote);
			
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
			String position = legislator.party + "-" + legislator.state;
			return legislator.last_name + ", " + legislator.firstName() + " [" + position + "]";
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
		LoadRollTask loadRollTask, loadVotersTask;
		Roll roll;
		Map<String,Roll.Vote> voters;
		Map<String,LoadPhotoTask> loadPhotoTasks;
		String currentTab;
		
		public RollInfoHolder(LoadRollTask loadRollTask, Roll roll, LoadRollTask loadVotersTask, Map<String,Roll.Vote> voters, Map<String,LoadPhotoTask> loadPhotoTasks, String currentTab) {
			this.loadRollTask = loadRollTask;
			this.roll = roll;
			this.loadVotersTask = loadVotersTask;
			this.voters = voters;
			this.loadPhotoTasks = loadPhotoTasks;
			this.currentTab = currentTab;
		}
	}
}