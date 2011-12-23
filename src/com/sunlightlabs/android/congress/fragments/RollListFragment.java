package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.RollService;

public class RollListFragment extends ListFragment {
	
	public static final int PER_PAGE = 20;
	
	public static final int ROLLS_VOTER = 0;
	public static final int ROLLS_RECENT = 1;
	public static final int ROLLS_SEARCH_NEWEST = 2;
	public static final int ROLLS_SEARCH_RELEVANT = 3;
	
	private List<Roll> rolls;
	
	private Legislator voter;
	private int type;
	String query;
	
	public static RollListFragment forRecent() {
		RollListFragment frag = new RollListFragment();
		Bundle args = new Bundle();
		args.putInt("type", ROLLS_RECENT);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static RollListFragment forLegislator(Legislator legislator) {
		RollListFragment frag = new RollListFragment();
		Bundle args = new Bundle();
		args.putInt("type", ROLLS_VOTER);
		args.putSerializable("legislator", legislator);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static RollListFragment forSearch(String query, int type) {
		RollListFragment frag = new RollListFragment();
		Bundle args = new Bundle();
		args.putInt("type", type);
		args.putString("query", query);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public RollListFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		type = args.getInt("type", ROLLS_VOTER);
		voter = (Legislator) args.getSerializable("legislator");
		query = args.getString("query");
		
		loadRolls();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_footer, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (rolls != null)
			displayRolls();
	}
	
	public void setupControls() {
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});

		FragmentUtils.setLoading(this, R.string.votes_loading);
	}
	
	private void setupSubscription() {
		Subscription subscription = null;
		
		if (type == ROLLS_VOTER)
			subscription = new Subscription(voter.id, Subscriber.notificationName(voter), "RollsLegislatorSubscriber", voter.chamber);
		else if (type == ROLLS_RECENT)
			subscription = new Subscription("RecentVotes", "Recent Votes", "RollsRecentSubscriber", null);
		
		if (subscription != null)
			Footer.setup(this, subscription, rolls);
	}
	
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		startActivity(Utils.rollIntent(getActivity(), (Roll) parent.getItemAtPosition(position)));
	}
	

	private void refresh() {
		rolls = null;
		FragmentUtils.setLoading(this, R.string.votes_loading);
		FragmentUtils.showLoading(this);
		loadRolls();
	}

	public void loadRolls() {
		new LoadRollsTask(this).execute();
	}
	
	public void onLoadRolls(List<Roll> rolls) {
		this.rolls = rolls;
		if (isAdded())
			displayRolls();
	}
	
	public void onLoadRolls(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.votes_error);
	}
	
	public void displayRolls() {
		if (rolls.size() > 0) {
			setListAdapter(new RollAdapter(this, rolls));
			setupSubscription();
		} else {
			if (type == ROLLS_SEARCH_NEWEST) {
				FragmentUtils.showEmpty(this, R.string.votes_empty_search_newest);
				setupSubscription();
			} else if (type == ROLLS_SEARCH_RELEVANT) {
				FragmentUtils.showEmpty(this, R.string.votes_empty_search_relevant);
				setupSubscription();
			} else if (type == ROLLS_VOTER) {
				FragmentUtils.showEmpty(this, R.string.votes_empty_voter);
				setupSubscription();
			} else // ROLLS_RECENT
				FragmentUtils.showRefresh(this, R.string.votes_error); // should not happen
		}
	}

	
	
//	public void onLoadRolls(List<Roll> newRolls) {
//		// if this is the first page of rolls, set up the subscription
//		if (rolls.size() == 0) {
//			if (newRolls.size() == 0) {
//				Utils.showBack(this, R.string.empty_votes);
//				setupSubscription(); // this extra call should get removed when we refactor pagination
//				return;
//			}
//		}
//		
//		if (rolls.size() > 0) {
//			int lastIndex = rolls.size() - 1;
//			if (rolls.get(lastIndex) == null)
//				rolls.remove(lastIndex);
//		}
//
//		rolls.addAll(newRolls);
//		
//		if (newRolls.size() == PER_PAGE)
//			rolls.add(null);
//
//		((RollAdapter) getListAdapter()).notifyDataSetChanged();
//		
//		setupSubscription();
//	}
//
//	public void onLoadRolls(CongressException exception) {
//		if (rolls.size() > 0) {
//			
//			loading.getLoading().setVisibility(View.GONE);
//			loading.getRetryContainer().setVisibility(View.VISIBLE);
//			
//			Button retry = loading.getRetry();
//			retry.setOnClickListener(new View.OnClickListener() {
//				public void onClick(View v) {
//					loading.getRetryContainer().setVisibility(View.GONE);
//					loading.getLoading().setVisibility(View.VISIBLE);
//					loadRolls();
//				}
//			});
//
//		} else
//			Utils.showBack(this, R.string.error_connection);
//	}


	private class LoadRollsTask extends AsyncTask<Void,Void,List<Roll>> {
		private RollListFragment context;
		private CongressException exception;

		public LoadRollsTask(RollListFragment context) {
			this.context = context;
			FragmentUtils.setupRTC(context);
		}

		@Override
		public List<Roll> doInBackground(Void... nothing) {
			try {
				int page = 1;

				Map<String,String> params = new HashMap<String,String>();
				
				switch (context.type) {
				case ROLLS_VOTER:
					return RollService.latestVotes(context.voter.id, context.voter.chamber, page, PER_PAGE);
				case ROLLS_RECENT:
					return RollService.latestVotes(page, PER_PAGE);
				case ROLLS_SEARCH_NEWEST:
					params.put("order", "voted_at");
					return RollService.search(context.query, params, page, PER_PAGE);
				case ROLLS_SEARCH_RELEVANT:
					params.put("order", "_score");
					return RollService.search(context.query, params, page, PER_PAGE);
				default:
					throw new CongressException("Not sure what type of votes to find.");
				}
			} catch(CongressException exception) {
				this.exception = exception;
				return null;
			}
		}

		@Override
		public void onPostExecute(List<Roll> rolls) {
			if (exception != null)
				context.onLoadRolls(exception);
			else
				context.onLoadRolls(rolls);
		}
	}
	
	private static class RollAdapter extends ArrayAdapter<Roll> {
		private LayoutInflater inflater;
		private RollListFragment context;
		
		public RollAdapter(RollListFragment context, List<Roll> rolls) {
			super(context.getActivity(), 0, rolls);
			this.inflater = LayoutInflater.from(context.getActivity());
			this.context = context;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Roll roll = getItem(position);

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
			if (context.type == RollListFragment.ROLLS_VOTER) {
				Roll.Vote vote = roll.voter_ids.get(context.voter.bioguide_id);
				if (vote == null || vote.vote.equals(Roll.NOT_VOTING)) {
					msgView.setText("Did Not Vote");
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				} else if (vote.vote.equals(Roll.YEA)) {
					msgView.setText(vote.vote);
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				} else if (vote.vote.equals(Roll.NAY)) {
					msgView.setText(vote.vote);
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				} else if (vote.vote.equals(Roll.PRESENT)) {
					msgView.setText(vote.vote);
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				} else {
					msgView.setText(vote.vote);
					msgView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				} 
				
			} else
				msgView.setText(Utils.capitalize(roll.chamber) + " Roll No. " + roll.number);
			
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
			String breakdown;
			if (roll.otherVotes) {
				breakdown = "";
				Iterator<Integer> iter = roll.voteBreakdown.values().iterator();
				while (iter.hasNext()) {
					breakdown += iter.next();
					if (iter.hasNext())
						breakdown += "-";
				}
			} else {
				breakdown = roll.voteBreakdown.get(Roll.YEA)+ "-" + roll.voteBreakdown.get(Roll.NAY);
				if (roll.voteBreakdown.get(Roll.PRESENT) > 0)
					breakdown += "-" + roll.voteBreakdown.get(Roll.PRESENT);
			}
			
			return roll.result + ", " + breakdown.toString();
		}
	}
	
}