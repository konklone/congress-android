package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.CommitteeService;

public class CommitteeList extends ListActivity {
	private List<Committee> committees;
	private String chamber;
	private LoadCommitteesTask loadCommitteesTask;
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);

		Utils.setupSunlight(this);

		Bundle extras = getIntent().getExtras();
		chamber = extras.getString("chamber");

		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder != null) {
			committees = holder.committees;
			loadCommitteesTask = holder.loadCommitteesTask;
			tracked = holder.tracked;
		}
		
		tracker = Analytics.start(this);
		if (!tracked) {
			Analytics.page(this, tracker, "/committees/" + chamber);
			tracked = true;
		}

		if (loadCommitteesTask == null)
			loadCommittees();
		else
			loadCommitteesTask.onScreenLoad(this);

		setupControls();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new Holder(committees, loadCommitteesTask, tracked);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Analytics.stop(tracker);
	}
	
	private void setupControls() {
		((Button) findViewById(R.id.back)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		Utils.setLoading(this, R.string.committees_loading);
	}

	@Override
	protected void onListItemClick(ListView parent, View v, int position, long id) {
		selectCommittee((Committee) parent.getItemAtPosition(position));
	}

	private void selectCommittee(Committee committee) {
		startActivity(new Intent(this, LegislatorList.class)
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
		displayCommittees();
	}

	public void displayCommittees() {
		if (committees.size() > 0)
			setListAdapter(new CommitteeAdapter(this, committees));
		else
			Utils.showBack(this, R.string.committees_empty);
	}

	private class ViewHolder {
		TextView name;
		String committeeId;

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ViewHolder))
				return false;
			ViewHolder oh = (ViewHolder) o;
			return oh != null && oh.committeeId.equals(this.committeeId);
		}
	}

	private class CommitteeAdapter extends ArrayAdapter<Committee> {
		LayoutInflater inflater;

		public CommitteeAdapter(CommitteeList context, List<Committee> items) {
			super(context, 0, items);
			this.inflater = LayoutInflater.from(context);
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
	}
	
	private class LoadCommitteesTask extends AsyncTask<String, Void, List<Committee>> {
		private CommitteeList context;

		public LoadCommitteesTask(CommitteeList context) {
			this.context = context;
		}

		public void onScreenLoad(CommitteeList context) {
			this.context = context;
		}

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
	
	private class Holder {
		List<Committee> committees;
		LoadCommitteesTask loadCommitteesTask;
		boolean tracked;
		
		Holder(List<Committee> committees, LoadCommitteesTask loadCommitteesTask, boolean tracked) {
			this.committees = committees;
			this.loadCommitteesTask = loadCommitteesTask;
			this.tracked = tracked;
		}
	}

}