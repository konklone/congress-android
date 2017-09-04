package com.sunlightlabs.android.congress.fragments;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.sunlightlabs.android.congress.utils.PaginationListener;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.RollService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RollListFragment extends ListFragment implements PaginationListener.Paginates {
	
	public static final int PER_PAGE = 20;
	
	public static final int ROLLS_VOTER = 0;
	public static final int ROLLS_RECENT = 1;
	
	private List<Roll> rolls;
	
	private Legislator voter;
	private int type;
	String query;
	
	PaginationListener pager;
	View loadingView;
	
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
	
	@Override
	public void onResume() {
		super.onResume();
		if (rolls != null)
			setupSubscription();
	}
	
	public void setupControls() {
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});
		
		loadingView = LayoutInflater.from(getActivity()).inflate(R.layout.loading_page, null);
		loadingView.setVisibility(View.GONE);
		getListView().addFooterView(loadingView);
		
		pager = new PaginationListener(this);
		getListView().setOnScrollListener(pager);

		FragmentUtils.setLoading(this, R.string.votes_loading);
	}
	
	private void setupSubscription() {
		Subscription subscription = null;
		
		if (type == ROLLS_VOTER)
			subscription = new Subscription(voter.bioguide_id, Subscriber.notificationName(voter), "RollsLegislatorSubscriber", voter.chamber);
		else if (type == ROLLS_RECENT)
			subscription = new Subscription("RecentVotes", "Recent Votes", "RollsRecentSubscriber", null);
		
		if (subscription != null)
			Footer.setup(this, subscription, rolls);
	}
	
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		Roll roll = (Roll) parent.getItemAtPosition(position);
		if (roll != null) // happened once somehow
			startActivity(Utils.rollIntent(getActivity(), roll.id));
	}
	

	private void refresh() {
		rolls = null;
		FragmentUtils.setLoading(this, R.string.votes_loading);
		FragmentUtils.showLoading(this);
		loadRolls();
	}
	
	@Override
	public void loadNextPage(int page) {
		getListView().setOnScrollListener(null);
		loadingView.setVisibility(View.VISIBLE);
		new LoadRollsTask(this, page).execute();
	}

	public void loadRolls() {
		new LoadRollsTask(this, 1).execute();
	}
	
	public void onLoadRolls(List<Roll> rolls, int page) {
		if (!isAdded())
			return;
		
		if (page == 1) {
			this.rolls= rolls;
			displayRolls();
		} else {
			this.rolls.addAll(rolls);
			loadingView.setVisibility(View.GONE);
			((RollAdapter) getListAdapter()).notifyDataSetChanged();
		}
		
		// only re-enable the pagination if we got a full page back
		if (rolls.size() >= PER_PAGE)
			getListView().setOnScrollListener(pager);
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
			if (type == ROLLS_VOTER) {
				FragmentUtils.showEmpty(this, R.string.votes_empty_voter);
				setupSubscription();
			} else // ROLLS_RECENT
				FragmentUtils.showRefresh(this, R.string.votes_error); // should not happen
		}
	}


	private class LoadRollsTask extends AsyncTask<Void,Void,List<Roll>> {
		private RollListFragment context;
		private CongressException exception;
		int page;

		public LoadRollsTask(RollListFragment context, int page) {
			this.context = context;
			this.page = page;
			FragmentUtils.setupAPI(context);
		}

		@Override
		public List<Roll> doInBackground(Void... nothing) {
			try {

				Map<String,String> params = new HashMap<String,String>();
				
				switch (context.type) {
				case ROLLS_VOTER:
					return RollService.latestMemberVotes(context.voter.bioguide_id, page);
				case ROLLS_RECENT:
					return RollService.latestVotes(page);
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
				context.onLoadRolls(rolls, page);
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
				holder.roll = (TextView) view.findViewById(R.id.chamber_number);
				holder.date = (TextView) view.findViewById(R.id.date);
				holder.question = (TextView) view.findViewById(R.id.question);
				holder.result = (TextView) view.findViewById(R.id.result);
                holder.details = (ViewGroup) view.findViewById(R.id.details);
                holder.detailsText = (TextView) view.findViewById(R.id.details_text);
				
				view.setTag(holder);
			} else
				holder = (ViewHolder) view.getTag();
			
			TextView msgView = holder.roll;

            // ?? why does this also activate for ROLLS_RECENT?
			if (context.voter != null && (context.type == ROLLS_VOTER || context.type == ROLLS_RECENT)) {
				if (roll.member_position == null || roll.member_position.equals(Roll.NOT_VOTING))
					msgView.setText(R.string.votes_did_not_vote);
				else
					msgView.setText(roll.member_position);
				
			} else
				msgView.setText(Utils.capitalize(roll.chamber));
			
			holder.roll = msgView;
			
			shortDate(holder.date, roll.voted_at);
			
			holder.question.setText(Utils.truncate(roll.question, 200));
			holder.result.setText(resultFor(roll));

            if (roll.bill_id != null && roll.bill_title != null) {
                holder.details.setVisibility(View.VISIBLE);

                String title;
                if (roll.bill_title != null)
                    title = Utils.truncate(roll.bill_title, 200);
                else
                    title = "(untitled)";

                holder.detailsText.setText(Bill.formatCode(roll.bill_id) + ": " + title);
            }

			return view;
		}

        static class ViewHolder {
			TextView roll, date, question, result, detailsText;
            ViewGroup details;
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
		
		private void shortDate(TextView view, Date date) {
			longDate(view, date);
		}
		
		private void longDate(TextView view, Date date) {
			view.setTextSize(14);
			view.setText(new SimpleDateFormat("MMM d, ''yy").format(date).toUpperCase());
		}
	}
	
}