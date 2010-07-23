package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import android.app.ListActivity;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.RollService;

public class RollList extends ListActivity {
	private static final int ROLLS = 20;
	
	public static final int ROLLS_VOTER = 0;
	
	private ArrayList<Roll> rolls;
	private LoadRollsTask loadRollsTask;

	private Legislator voter;
	private int type;
	
	private LoadingWrapper loading;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled);

		Bundle extras = getIntent().getExtras();
		type = extras.getInt("type", ROLLS_VOTER);
		voter = (Legislator) extras.getSerializable("voter");

		setupControls();

		RollListHolder holder = (RollListHolder) getLastNonConfigurationInstance();

		if (holder != null) {
			this.rolls = holder.rolls;
			this.loadRollsTask = holder.loadRollsTask;

			if (loadRollsTask != null)
				loadRollsTask.onScreenLoad(this);
		} else
			rolls = new ArrayList<Roll>();

		setListAdapter(new RollAdapter(this, rolls));

		if (rolls.size() == 0)
			loadRolls();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new RollListHolder(rolls, loadRollsTask);
	}

	public void setupControls() {
		((Button) findViewById(R.id.back)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		Utils.setLoading(this, R.string.rolls_loading);
		switch (type) {
		case ROLLS_VOTER:
		default:
			Utils.setTitle(this, "Latest Votes By\n" + voter.titledName(), R.drawable.rolls);
			Utils.setTitleSize(this, 18);
			break;
		}
	}

	protected void onListItemClick(ListView parent, View v, int position, long id) {
		Roll roll = (Roll) parent.getItemAtPosition(position);
		if (roll != null)
			startActivity(Utils.rollIntent(this, roll));
	}

	public void loadRolls() {
		if (loadRollsTask == null)
			loadRollsTask = (LoadRollsTask) new LoadRollsTask(this).execute();
	}


	public void onLoadRolls(ArrayList<Roll> newRolls) {
		if (rolls.size() == 0 && newRolls.size() == 0) {
			Utils.showBack(this, R.string.empty_rolls);
			return;
		}
		
		// remove the placeholder and add the new bills in the array
		if (rolls.size() > 0) {
			int lastIndex = rolls.size() - 1;
			if (rolls.get(lastIndex) == null) {
				rolls.remove(lastIndex);
			}
		}

		rolls.addAll(newRolls);

		// if we got back a full page of bills, there may be more yet to come
		if (newRolls.size() == ROLLS)
			rolls.add(null);

		((RollAdapter) getListAdapter()).notifyDataSetChanged();
	}

	public void onLoadRolls(CongressException exception) {
		if (rolls.size() > 0) {
			
			loading.getLoading().setVisibility(View.GONE);
			loading.getRetryContainer().setVisibility(View.VISIBLE);
			
			Button retry = loading.getRetry();
			retry.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					loading.getRetryContainer().setVisibility(View.GONE);
					loading.getLoading().setVisibility(View.VISIBLE);
					loadRolls();
				}
			});

		} else
			Utils.showBack(this, R.string.error_connection);
	}

	private static class RollAdapter extends ArrayAdapter<Roll> {
		private LayoutInflater inflater;
		private RollList context;
		private Resources resources;
		
		private static final int ROLL = 0;
		private static final int LOADING = 1;

		public RollAdapter(RollList context, ArrayList<Roll> rolls) {
			super(context, 0, rolls);
			this.inflater = LayoutInflater.from(context);
			this.context = context;
			this.resources = context.getResources();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return !((position == getCount() - 1) && getItem(position) == null);
		}
		
		@Override
		public int getItemViewType(int position) {
			if (getItem(position) != null)
				return ROLL;
			else
				return LOADING;
		}
		
		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Roll roll = getItem(position);

			if (roll == null)
				return getLoadingView();
			else
				return getRollView(roll, convertView);
		}

		private View getLoadingView() {
			context.loadRolls();
			context.loading = new LoadingWrapper(inflater.inflate(R.layout.loading_retry, null));
			return context.loading.getBase();
		}

		private View getRollView(Roll roll, View view) {
			ViewHolder holder;
			if (view == null) {
				view = inflater.inflate(R.layout.roll_item, null);
				
				holder = new ViewHolder();
				holder.roll = (TextView) view.findViewById(R.id.roll);
				holder.date = (TextView) view.findViewById(R.id.date);
				holder.question = (TextView) view.findViewById(R.id.question);
				holder.result = (TextView) view.findViewById(R.id.result);
				
				view.setTag(holder);
			} else
				holder = (ViewHolder) view.getTag();
			
			TextView msgView = holder.roll;
			if (context.type == RollList.ROLLS_VOTER) {
				Roll.Vote vote = roll.voter_ids.get(context.voter.bioguide_id);
				if (vote == null || vote.vote == Roll.NOT_VOTING) {
					msgView.setText("Did Not Vote");
					msgView.setTextColor(resources.getColor(android.R.color.white));
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				} else if (vote.vote == Roll.OTHER) {
					msgView.setText(vote.vote_name);
					msgView.setTextColor(resources.getColor(android.R.color.white));
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				} else if (vote.vote == Roll.YEA) {
					msgView.setText("Yea");
					msgView.setTextColor(resources.getColor(R.color.yea));
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				} else if (vote.vote == Roll.NAY) {
					msgView.setText("Nay");
					msgView.setTextColor(resources.getColor(R.color.nay));
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				} else if (vote.vote == Roll.PRESENT) {
					msgView.setText("Present");
					msgView.setTextColor(resources.getColor(android.R.color.white));
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				}
				
			} else
				msgView.setText(Utils.capitalize(roll.chamber) + "Roll No. " + roll.number);
			
			holder.roll = msgView;
			
			holder.date.setText(new SimpleDateFormat("MMM dd, yyyy").format(roll.voted_at));
			holder.question.setText(roll.question);
			holder.result.setText(resultFor(roll));
				
			return view;
		}
		
		static class ViewHolder {
			TextView roll, date, question, result;
		}
		
		private String resultFor(Roll roll) {
			// if a roll call has non-standard votes, it's the House election of the Speaker - only known exception
			String breakdown;
			if (roll.otherVotes.isEmpty()) {
				breakdown = roll.yeas + "-" + roll.nays;
				if (roll.present > 0)
					breakdown += "-" + roll.present;
			} else {
				breakdown = "";
				Iterator<Integer> iter = roll.otherVotes.values().iterator();
				while (iter.hasNext()) {
					int val = iter.next().intValue();
					breakdown += "" + val;
					if (iter.hasNext())
						breakdown += "-";
				}
			}
			
			return roll.result + ", " + breakdown;
		}
	}
	
	private class LoadRollsTask extends AsyncTask<Void,Void,ArrayList<Roll>> {
		private RollList context;
		private CongressException exception;

		public LoadRollsTask(RollList context) {
			this.context = context;
			Utils.setupDrumbone(context);
		}

		public void onScreenLoad(RollList context) {
			this.context = context;
		}

		@Override
		public ArrayList<Roll> doInBackground(Void... nothing) {
			try {
				int page = (context.rolls.size() / ROLLS) + 1;

				switch (context.type) {
				case ROLLS_VOTER:
					return RollService.latestVotes(context.voter, ROLLS, page);
				default:
					throw new CongressException("Not sure what type of bills to find.");
				}
			} catch(CongressException exception) {
				this.exception = exception;
				return null;
			}
		}

		@Override
		public void onPostExecute(ArrayList<Roll> rolls) {
			context.loadRollsTask = null;

			if (exception != null)
				context.onLoadRolls(exception);
			else
				context.onLoadRolls(rolls);
		}
	}

	static class RollListHolder {
		ArrayList<Roll> rolls;
		LoadRollsTask loadRollsTask;

		public RollListHolder(ArrayList<Roll> rolls, LoadRollsTask loadRollsTask) {
			this.rolls = rolls;
			this.loadRollsTask = loadRollsTask;
		}
	}
	
	static class LoadingWrapper {
		private View base, loading, retryContainer;
		private Button retry;

		public LoadingWrapper(View base) {
			this.base = base;
		}
		
		public View getLoading() {
			return loading == null ? loading = base.findViewById(R.id.loading_layout) : loading;
		}
		
		public Button getRetry() {
			return retry == null ? retry = (Button) base.findViewById(R.id.retry) : retry;
		}
		
		public View getRetryContainer() {
			return retryContainer == null ? retryContainer = base.findViewById(R.id.retry_container) : retryContainer;
		}
		
		public View getBase() {
			return base;
		}
	}

}