package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Drumbone;
import com.sunlightlabs.congress.java.Legislator;

public class BillList extends ListActivity {
	private static final int BILLS = 20;
	
	ArrayList<Bill> bills;
	LoadBillsTask loadBillsTask;
	
	LinearLayout introduced;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.legislator_list);
		
		Drumbone.apiKey = getResources().getString(R.string.sunlight_api_key);
		Drumbone.baseUrl = getResources().getString(R.string.drumbone_base_url);
		
		setupControls();
		
		MainActivityHolder holder = (MainActivityHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.bills = holder.bills;
			this.loadBillsTask = holder.loadBillsTask;
			if (loadBillsTask != null)
				loadBillsTask.onScreenLoad(this);
		}
		
		loadBills();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new MainActivityHolder(bills, loadBillsTask);
	}
	
	public void setupControls() {
		setTitle("Last " + BILLS + " Introduced Bills");
	}
	
	protected void onListItemClick(ListView parent, View v, int position, long id) {
    	Bill bill = (Bill) parent.getItemAtPosition(position);
    	startActivity(Utils.billIntent(this, bill));
    }
	
	public void loadBills() {
		if (loadBillsTask == null) {
			if (bills == null)
				loadBillsTask = (LoadBillsTask) new LoadBillsTask(this).execute();
			else
				displayBills();
		}
	}
	
	public void onLoadBills(ArrayList<Bill> bills) {
		this.bills = bills;
		displayBills();
	}
	
	public void onLoadBills(CongressException exception) {
		Utils.alert(this, exception);
		this.bills = new ArrayList<Bill>();
		displayBills();
	}
	
	public void displayBills() {
		setListAdapter(new BillAdapter(this, bills));
	}
	
	private class LoadBillsTask extends AsyncTask<Void,Void,ArrayList<Bill>> {
		private BillList context;
		private CongressException exception;
		private ProgressDialog dialog;
		
		public LoadBillsTask(BillList context) {
			this.context = context;
		}
		
		@Override
    	protected void onPreExecute() {
    		loadingDialog();
    	}
		
		public void onScreenLoad(BillList context) {
			this.context = context;
			loadingDialog();
		}
		
		@Override
		public ArrayList<Bill> doInBackground(Void... nothing) {
			try {
				return Bill.recentlyIntroduced(BILLS);
			} catch(CongressException exception) {
				this.exception = exception;
				return null;
			}
		}
		
		@Override
		public void onPostExecute(ArrayList<Bill> bills) {
			if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
			
			context.loadBillsTask = null;
			
			if (exception != null && bills == null)
				context.onLoadBills(exception);
			else
				context.onLoadBills(bills);
		}
		
		public void loadingDialog() {
        	dialog = new ProgressDialog(context);
        	dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        	dialog.setMessage("Loading bills...");
        	
        	dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cancel(true);
    				context.finish();
				}
			});
        	
        	dialog.show();
        }
	}
	
	private class BillAdapter extends ArrayAdapter<Bill> {
		LayoutInflater inflater;

	    public BillAdapter(Activity context, ArrayList<Bill> bills) {
	        super(context, 0, bills);
	        inflater = LayoutInflater.from(context);
	    }

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view;
			
			if (convertView == null)
				view = (LinearLayout) inflater.inflate(R.layout.bill_item, null);
			else
				view = (LinearLayout) convertView;
				
			Bill bill = getItem(position);
			
			((TextView) view.findViewById(R.id.byline)).setText(byline(bill));
			((TextView) view.findViewById(R.id.title)).setText(Utils.truncate(bill.displayTitle(), 300));
			
			view.setTag(bill);
			
			return view;
		}
		
		private String byline(Bill bill) {
			String date = new SimpleDateFormat("MMM dd").format(bill.introduced_at);
			
			Legislator sponsor = bill.sponsor;
			String name = sponsor.title + ". " + sponsor.last_name;
			if (sponsor.name_suffix != null && sponsor.name_suffix.length() > 0)
				name += " " + sponsor.name_suffix;
			
			return date + " - " + name + " introduced " + Bill.formatCode(bill.code) + ":";
		}
	}
	
	static class MainActivityHolder {
		ArrayList<Bill> bills;
		LoadBillsTask loadBillsTask;
		public MainActivityHolder(ArrayList<Bill> bills, LoadBillsTask loadBillsTask) {
			this.bills = bills;
			this.loadBillsTask = loadBillsTask;
		}
	}
}