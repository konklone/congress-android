package com.sunlightlabs.android.congress.fragments;

import java.text.SimpleDateFormat;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
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
		
		if (bill.passage_votes != null)
			displayBill();
	}
	
	private void setupSubscription() {
		Footer.setup(this, new Subscription(bill.id, Subscriber.notificationName(bill), "VotesBillSubscriber", bill.id), bill.passage_votes);
	}

	public void loadBill() {
		new LoadBillTask(this, bill.id).execute("passage_votes");
	}
	
	public void onLoadBill(Bill bill) {
		this.bill.passage_votes = bill.passage_votes;
		if (isAdded())
			displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.error_connection);
	}
	
	public void displayBill() {
		if (bill.passage_votes.size() > 0)
			setListAdapter(new BillVoteAdapter(this, bill.passage_votes));
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
        public boolean isEnabled(int position) {
        	return (getItem(position)).roll_id != null;
        }
        
        @Override
        public boolean areAllItemsEnabled() {
        	return false;
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
			
			String timestamp = new SimpleDateFormat("MMM dd, yyyy").format(vote.voted_at);
			((TextView) view.findViewById(R.id.voted_at)).setText(timestamp);
			((TextView) view.findViewById(R.id.chamber)).setText("the " + Utils.capitalize(vote.chamber));
			
			TextView resultView = (TextView) view.findViewById(R.id.result);
			String result = vote.result;
			if (result.equals("pass")) {
				resultView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				resultView.setText("Passed");
			} else if (result.equals("fail")) {
				resultView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				resultView.setText("Failed");
			}
			
			String roll_id = vote.roll_id;
			TextView typeMessage = (TextView) view.findViewById(R.id.type_message);
			if (roll_id != null) {
				typeMessage.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				typeMessage.setTextColor(resources.getColor(R.color.text));
				typeMessage.setText(R.string.bill_vote_roll);
				view.setTag(vote.roll_id);
			} else {
				typeMessage.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				typeMessage.setTextColor(resources.getColor(R.color.text_grey));
				typeMessage.setText(R.string.bill_vote_not_roll);
				view.setTag(null);
			}
			
			return view;
		}

    }
}