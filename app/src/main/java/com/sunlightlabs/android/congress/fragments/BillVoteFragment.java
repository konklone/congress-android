package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.List;

import android.app.Fragment;
import android.app.ListFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class BillVoteFragment extends ListFragment implements LoadBillTask.LoadsBill {
	private Bill bill;

	public static BillVoteFragment create(Bill bill) {
		BillVoteFragment frag = new BillVoteFragment();
		Bundle args = new Bundle();
		
		args.putSerializable("bill", bill);
		
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public BillVoteFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		bill = (Bill) args.getSerializable("bill");
		
		loadBill();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_footer, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		FragmentUtils.setLoading(this, R.string.bill_votes_loading);
		
		if (bill.votes != null)
			displayBill();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (bill.votes != null)
			setupSubscription();
	}
	
	private void setupSubscription() {
		Footer.setup(this, new Subscription(bill.id, Subscriber.notificationName(bill), "VotesBillSubscriber", bill.id), bill.votes);
	}

	public void loadBill() {
		new LoadBillTask(this, bill.id).execute("votes");
	}
	
	public void onLoadBill(Bill bill) {
		this.bill.votes = bill.votes;
		if (isAdded())
			displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.error_connection);
	}
	
	public void displayBill() {
		if (bill.votes.size() > 0)
			setListAdapter(new BillVoteAdapter(this, bill.votes));
		else
			FragmentUtils.showEmpty(this, R.string.bill_votes_empty);
		
		setupSubscription();
	}
	
	@Override
    public void onListItemClick(ListView l, View v, int position, long id) {
		String rollId = (String) v.getTag();
    	if (rollId != null)
    		startActivity(Utils.rollIntent(getActivity(), rollId));
    }
	
	protected class BillVoteAdapter extends ArrayAdapter<Bill.Vote> {
    	LayoutInflater inflater;
    	Resources resources;

        public BillVoteAdapter(Fragment context, List<Bill.Vote> items) {
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
			if (view == null)
				view = inflater.inflate(R.layout.bill_vote, null);
			
			Bill.Vote vote = getItem(position);
			
			String timestamp = new SimpleDateFormat("MMM dd, yyyy").format(vote.voted_at).toUpperCase();
			((TextView) view.findViewById(R.id.date)).setText(timestamp);
			
			TextView resultView = (TextView) view.findViewById(R.id.result);
			String result = vote.result;
			String resultDisplay;
			if (result.equals("pass"))
				resultDisplay = "Passed";
			else // if (result.equals("fail"))
				resultDisplay = "Failed";
				
			resultView.setText(resultDisplay + " the " + Utils.capitalize(vote.chamber));
			
			String roll_id = vote.roll_id;
			TextView typeMessage = (TextView) view.findViewById(R.id.type_message);
			if (roll_id != null) {
				typeMessage.setTextColor(resources.getColor(R.color.text));
				typeMessage.setText(R.string.bill_vote_roll);
				view.setTag(vote.roll_id);
			} else {
				typeMessage.setTextColor(resources.getColor(R.color.text_grey));
				typeMessage.setText(R.string.bill_vote_not_roll);
				view.setTag(null);
			}
			
			return view;
		}

    }
}