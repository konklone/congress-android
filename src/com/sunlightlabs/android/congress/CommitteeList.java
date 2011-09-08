package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.CommitteeService;

public class CommitteeList extends FragmentActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String chamber = getIntent().getExtras().getString("chamber");
		
		Analytics.track(this, "/committees/" + chamber);
		
		if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            CommitteeListFragment list = CommitteeListFragment.create(chamber);
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, list).commit();
        }
	}

	
	public static class CommitteeListFragment extends ListFragment {
		private List<Committee> committees;
		private String chamber;
		private LoadCommitteesTask loadCommitteesTask;
		
		public static CommitteeListFragment create(String chamber) {
			CommitteeListFragment frag = new CommitteeListFragment();
			Bundle args = new Bundle();
			args.putString("chamber", chamber);
			frag.setArguments(args);
			
			// could set this in the Activity, but right now I think a fragment has the right to know 
			// whether it'll be going through the lifecycle of a retained fragment or not
			frag.setRetainInstance(true);
			
			return frag;
		}
		
		@Override
		public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.list, null);
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			this.chamber = getArguments().getString("chamber");
			
			Utils.setupSunlight(getActivity());
			
			if (loadCommitteesTask == null)
				loadCommittees();
			
			setupControls();
		}
		
		private void setupControls() {
			FragmentUtils.setLoading(this, R.string.committees_loading);
		}
		
		@Override
		public void onListItemClick(ListView parent, View v, int position, long id) {
			selectCommittee((Committee) parent.getItemAtPosition(position));
		}
	
		private void selectCommittee(Committee committee) {
			startActivity(new Intent(getActivity(), LegislatorList.class)
				.putExtra("type", LegislatorList.SEARCH_COMMITTEE)
				.putExtra("committeeId", committee.id)
				.putExtra("committeeName", committee.name));
		}
		
		public void loadCommittees() {
			if (committees == null)
				loadCommitteesTask = (LoadCommitteesTask) new LoadCommitteesTask(this).execute(chamber);
			else
				displayCommittees();
		}
		
		public void onLoadCommittees(List<Committee> committees) {
			loadCommitteesTask = null;
			Collections.sort(committees);
			this.committees = committees;
			
			if (isAdded()) // activity may not still be around any more by the time the loading task finishes 
				displayCommittees();
		}
	
		public void displayCommittees() {
			if (committees.size() > 0)
				setListAdapter(new CommitteeAdapter(this, committees));
			else
				FragmentUtils.showEmpty(this, R.string.committees_empty);
		}
	
		private static class CommitteeAdapter extends ArrayAdapter<Committee> {
			LayoutInflater inflater;
	
			public CommitteeAdapter(CommitteeListFragment context, List<Committee> items) {
				super(context.getActivity(), 0, items);
				this.inflater = LayoutInflater.from(context.getActivity());
			}
	
			@Override
			public int getViewTypeCount() {
				return 1;
			}
	
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = null;
				ViewHolder holder;
				if (convertView == null) {
					view = inflater.inflate(R.layout.profile_committee, null);
					holder = new ViewHolder();
					holder.name = (TextView) view.findViewById(R.id.name);
					view.setTag(holder);
				}
				else {
					view = convertView;
					holder = (ViewHolder) view.getTag();
				}
	
				Committee committee = getItem(position);
				holder.committeeId = committee.id;
				holder.name.setText(committee.name);
	
				return view;
			}
			
			static class ViewHolder {
				TextView name;
				String committeeId;
	
				@Override
				public boolean equals(Object o) {
					ViewHolder oh = (ViewHolder) o;
					return oh != null && oh.committeeId.equals(this.committeeId);
				}
			}
		}
		
		private class LoadCommitteesTask extends AsyncTask<String, Void, List<Committee>> {
			private CommitteeListFragment context;
	
			public LoadCommitteesTask(CommitteeListFragment context) {
				this.context = context;
			}
	
//			public void onScreenLoad(CommitteeListFragment context) {
//				this.context = context;
//			}
	
			@Override
			protected List<Committee> doInBackground(String... params) {
				List<Committee> result = new ArrayList<Committee>();
				String chamber = params[0];
				try {
					result = CommitteeService.getAll(chamber);
				} catch (CongressException e) {
					Log.e(Utils.TAG, "There has been an exception while getting the committees for chamber "
							+ chamber + ": " + e.getMessage());
				}
				return result;
			}
	
			@Override
			protected void onPostExecute(List<Committee> result) {
				context.onLoadCommittees(result);
			}
	
		}
	}
}