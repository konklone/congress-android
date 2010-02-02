package com.sunlightlabs.android.congress;

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;

public class MainActivity extends ListActivity {
	private static final int RECENT_BILLS = 3;
	
	ArrayList<Bill> recentBills;
	RecentBillsTask recentBillsTask;
	
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
		setListAdapter(new ArrayAdapter<Bill>(this, android.R.layout.simple_list_item_1, recentBills));
	}
	
	public void setupControls() {
		
	}
	
	private class RecentBillsTask extends AsyncTask<Void,Void,ArrayList<Bill>> {
		private MainActivity context;
		private CongressException exception;
		private ProgressDialog dialog;
		
		public RecentBillsTask(MainActivity context) {
			this.context = context;
			loadingDialog();
		}
		
		public void onScreenLoad(MainActivity context) {
			this.context = context;
			loadingDialog();
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
			if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
			context.recentBillsTask = null;
			
			if (exception != null && bills == null)
				context.onLoadRecentBills(exception);
			else
				context.onLoadRecentBills(bills);
		}
		
		public void loadingDialog() {
        	dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Loading recent bills...");
            
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cancel(true);
    				finish();
				}
			});
            
            dialog.show();
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