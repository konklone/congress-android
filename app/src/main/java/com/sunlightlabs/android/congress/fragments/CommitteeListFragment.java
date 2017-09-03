package com.sunlightlabs.android.congress.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.CommitteePager;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.CommitteeService;
import com.sunlightlabs.congress.services.LegislatorService;

public class CommitteeListFragment extends ListFragment {
	public static final int CHAMBER = 1;
	public static final int LEGISLATOR = 2;
	public static final int COMMITTEE = 3;
	
	private List<Committee> committees;
	private int type;
	private String chamber;
	private Legislator legislator;
	private Committee committee;
	
	public static CommitteeListFragment forChamber(String chamber) {
		CommitteeListFragment frag = new CommitteeListFragment();
		Bundle args = new Bundle();
		args.putInt("type", CHAMBER);
		args.putString("chamber", chamber);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static CommitteeListFragment forLegislator(Legislator legislator) {
		CommitteeListFragment frag = new CommitteeListFragment();
		Bundle args = new Bundle();
		args.putInt("type", LEGISLATOR);
		args.putSerializable("legislator", legislator);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static CommitteeListFragment forCommittee(Committee committee) {
		CommitteeListFragment frag = new CommitteeListFragment();
		Bundle args = new Bundle();
		args.putInt("type", COMMITTEE);
		args.putSerializable("committee", committee);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public CommitteeListFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		type = args.getInt("type");
		chamber = args.getString("chamber");
		legislator = (Legislator) args.getSerializable("legislator");
		committee = (Committee) args.getSerializable("committee");
		
		if (type == CHAMBER)
			new LoadCommitteesTask(this).execute(chamber);
		else if (type == LEGISLATOR)
			new LoadCommitteesTask(this).execute(legislator.bioguide_id);
		else if (type == COMMITTEE)
			new LoadCommitteesTask(this).execute(committee.id);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		FragmentUtils.setLoading(this, R.string.committees_loading);
		
		if (committees != null)
			displayCommittees();
	}
	
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		selectCommittee((Committee) parent.getItemAtPosition(position));
	}

	private void selectCommittee(Committee committee) {
		startActivity(new Intent(getActivity(), CommitteePager.class)
			.putExtra("committee", committee));
	}
	
	public void onLoadCommittees(List<Committee> committees) {
		this.committees = committees;
		
		if (isAdded()) 
			displayCommittees();
	}
	
	public void onLoadCommittees(CongressException exception) {
		if (isAdded())
			FragmentUtils.showEmpty(this, exception.getMessage());
	}

	public void displayCommittees() {
		if (committees.size() > 0)
			setListAdapter(new CommitteeAdapter(this, committees));
		else {
			int empty;
			if (type == CHAMBER)
				empty = R.string.committees_empty;
			else if (type == LEGISLATOR)
				empty = R.string.legislator_no_committees;
			else // if (type == COMMITTEE)
				empty = R.string.committees_no_subcommittees;
			
			FragmentUtils.showEmpty(this, empty);
		}
	}

	private static class CommitteeAdapter extends ArrayAdapter<Committee> {
		LayoutInflater inflater;
		CommitteeListFragment context;

		public CommitteeAdapter(CommitteeListFragment context, List<Committee> items) {
			super(context.getActivity(), 0, items);
			this.inflater = LayoutInflater.from(context.getActivity());
			this.context = context;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Committee committee = getItem(position);
			
			View view;
			if (committee.subcommittee && context.type != COMMITTEE)
				view = inflater.inflate(R.layout.committee_item_sub, null);
			else
				view = inflater.inflate(R.layout.committee_item, null);
			
			TextView name = (TextView) view.findViewById(R.id.name);
			name.setText(committee.name);
			
			return view;
		}
	}
	
	private static class LoadCommitteesTask extends AsyncTask<String, Void, List<Committee>> {
		private CommitteeListFragment context;
		private CongressException exception;

		public LoadCommitteesTask(CommitteeListFragment context) {
			FragmentUtils.setupAPI(context);
			this.context = context;
		}

		@Override
		protected List<Committee> doInBackground(String... params) {
			if (context.type == CHAMBER)
				return forChamber(params[0]);
			else if (context.type == LEGISLATOR)
				return forLegislator(params[0]);
			else if (context.type == COMMITTEE)
				return forCommittee(params[0]);
			else
				return null;
		}
		
		private List<Committee> forLegislator(String bioguideId) {
			List<Committee> committees;
			try {
				committees = LegislatorService.find(bioguideId).committees;
			} catch (CongressException e) {
				this.exception = new CongressException(e, "Error loading committees.");
				return null;
			}
			
			List<Committee> chamber = new ArrayList<Committee>();
			List<Committee> joint = new ArrayList<Committee>();
			
			for (int i=0; i<committees.size(); i++) {
				if (committees.get(i).chamber.equals("joint"))
					joint.add(committees.get(i));
				else
					chamber.add(committees.get(i));
			}
			
			// sort by their committee ID, not the name
			// but make an exception to put parent committees above their subcommittees
			// (they'd naturally end up below)
			Collections.sort(chamber, new Comparator<Committee>() {
				@Override
				public int compare(Committee a, Committee b) {
					if (a.subcommittee && !b.subcommittee && a.parent_committee_id.equals(b.id))
						return 1;
					else if (b.subcommittee && !a.subcommittee && b.parent_committee_id.equals(a.id))
						return -1;
					else
						return a.id.compareTo(b.id);
				}
			});
			
			Collections.sort(joint, new Comparator<Committee>() {
				@Override
				public int compare(Committee a, Committee b) {
					return a.id.compareTo(b.id);
				}
			});
			
			List<Committee> result = new ArrayList<Committee>();
			result.addAll(chamber);
			result.addAll(joint);
			
			return result;
		}
		
		private List<Committee> forChamber(String chamber) {
			List<Committee> result = new ArrayList<Committee>();
			try {
				result = CommitteeService.forChamber(chamber);
			} catch (CongressException e) {
				Log.e(Utils.TAG, "There has been an exception while getting the committees for chamber "
						+ chamber + ": " + e.getMessage());
			}
			
			Collections.sort(result);
			return result;
		}
		
		private List<Committee> forCommittee(String committeeId) {
			List<Committee> result = new ArrayList<Committee>();
			try {
				result = CommitteeService.find(committeeId).subcommittees;
			} catch (CongressException e) {
				Log.e(Utils.TAG, "There has been an exception while getting the subcommittees for  "
						+ committeeId + ": " + e.getMessage());
			}
			
			Collections.sort(result);
			return result;
		}

		@Override
		protected void onPostExecute(List<Committee> result) {
			if (result != null && exception == null)
				context.onLoadCommittees(result);
			else
				context.onLoadCommittees(exception);
		}

	}
}