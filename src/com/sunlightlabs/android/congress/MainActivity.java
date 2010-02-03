package com.sunlightlabs.android.congress;

import java.util.ArrayList;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;

public class MainActivity extends ListActivity {
	private static final int RECENT_BILLS = 3;
	
	ArrayList<Bill> recentBills;
	RecentBillsTask recentBillsTask;
	
	LinearLayout introduced;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setupControls();
		
		MainActivityHolder holder = (MainActivityHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.recentBills = holder.recentBills;
			this.recentBillsTask = holder.recentBillsTask;
			if (recentBillsTask != null)
				recentBillsTask.onScreenLoad(this);
		}
		
		loadRecentBills();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new MainActivityHolder(recentBills, recentBillsTask);
	}
	
	public void setupControls() {
		LayoutInflater inflater = LayoutInflater.from(this);
		introduced = (LinearLayout) inflater.inflate(R.layout.header_loading, null);
		((TextView) introduced.findViewById(R.id.header_text)).setText("Recently Introduced");
		((TextView) introduced.findViewById(R.id.loading_message)).setText("Loading bills...");
		
		MergeAdapter adapter = new MergeAdapter();
		adapter.addView(introduced);
		
		setListAdapter(adapter);
	}
	
	public void loadRecentBills() {
		if (recentBillsTask == null) {
			if (recentBills == null)
				recentBillsTask = (RecentBillsTask) new RecentBillsTask(this).execute();
			else
				displayRecentBills();
		}
	}
	
	public void onLoadRecentBills(ArrayList<Bill> bills) {
		this.recentBills = bills;
		displayRecentBills();
	}
	
	public void onLoadRecentBills(CongressException exception) {
		Utils.alert(this, exception);
		this.recentBills = new ArrayList<Bill>();
		displayRecentBills();
	}
	
	public void displayRecentBills() {
		if (recentBills.size() <= 0) {
			introduced.findViewById(R.id.loading_spinner).setVisibility(View.GONE);
			((TextView) introduced.findViewById(R.id.loading_message)).setText("Could not load bills.");
		} else {
			introduced.findViewById(R.id.loading).setVisibility(View.GONE);
			MergeAdapter adapter = (MergeAdapter) getListAdapter();
			adapter.addAdapter(new ArrayAdapter<Bill>(this, android.R.layout.simple_list_item_1, recentBills));
			setListAdapter(adapter);
		}
	}
	
	
	private class RecentBillsTask extends AsyncTask<Void,Void,ArrayList<Bill>> {
		private MainActivity context;
		private CongressException exception;
		
		public RecentBillsTask(MainActivity context) {
			this.context = context;
		}
		
		public void onScreenLoad(MainActivity context) {
			this.context = context;
		}
		
		@Override
		public ArrayList<Bill> doInBackground(Void... nothing) {
			try {
				return Bill.recentlyIntroduced(RECENT_BILLS);
			} catch(CongressException exception) {
				this.exception = exception;
				return null;
			}
		}
		
		@Override
		public void onPostExecute(ArrayList<Bill> bills) {
			context.recentBillsTask = null;
			
			if (exception != null && bills == null)
				context.onLoadRecentBills(exception);
			else
				context.onLoadRecentBills(bills);
		}
	}
	
	static class MainActivityHolder {
		ArrayList<Bill> recentBills;
		RecentBillsTask recentBillsTask;
		public MainActivityHolder(ArrayList<Bill> recentBills, RecentBillsTask recentBillsTask) {
			this.recentBills = recentBills;
			this.recentBillsTask = recentBillsTask;
		}
	}
}