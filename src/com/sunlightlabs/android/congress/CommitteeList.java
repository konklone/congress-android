package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.Collections;

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

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.CommitteeService;

public class CommitteeList extends ListActivity {
	public static final String TAG = "CONGRESS";

	private ArrayList<Committee> committees;
	private String chamber;
	private LoadCommitteesTask loadCommitteesTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);

		Utils.setupSunlight(this);

		Bundle extras = getIntent().getExtras();
		chamber = extras.getString("chamber");

		CommitteeListHolder holder = (CommitteeListHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			committees = holder.committees;
			loadCommitteesTask = holder.loadCommitteesTask;
			chamber = holder.chamber;
		}

		if (loadCommitteesTask == null)
			loadCommittees();
		else
			loadCommitteesTask.onScreenLoad(this);

		setupControls();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		CommitteeListHolder holder = new CommitteeListHolder();
		holder.committees = this.committees;
		holder.chamber = this.chamber;
		holder.loadCommitteesTask = this.loadCommitteesTask;
		return holder;
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
	
	public void onLoadCommittees(ArrayList<Committee> committees) {
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

		public CommitteeAdapter(CommitteeList context, ArrayList<Committee> items) {
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
	
	private class LoadCommitteesTask extends AsyncTask<String, Void, ArrayList<Committee>> {
		private CommitteeList context;

		public LoadCommitteesTask(CommitteeList context) {
			this.context = context;
		}

		public void onScreenLoad(CommitteeList context) {
			this.context = context;
		}

		@Override
		protected ArrayList<Committee> doInBackground(String... params) {
			ArrayList<Committee> result = new ArrayList<Committee>();
			String chamber = params[0];
			try {
				result = CommitteeService.getAll(chamber);
			} catch (CongressException e) {
				Log.e(TAG, "There has been an exception while getting the committees for chamber "
						+ chamber + ": " + e.getMessage());
			}
			return result;
		}

		@Override
		protected void onPostExecute(ArrayList<Committee> result) {
			context.onLoadCommittees(result);
		}

	}
	
	private class CommitteeListHolder {
		ArrayList<Committee> committees;
		LoadCommitteesTask loadCommitteesTask;
		String chamber;
	}

}