package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
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

public class BillList extends ListActivity {
	private static final int BILLS = 20;
	
	public static final int BILLS_RECENT = 0;
	public static final int BILLS_LAW = 1;
	public static final int BILLS_SPONSOR = 2;
	
	private ArrayList<Bill> bills;
	private LoadBillsTask loadBillsTask;
	
	private String sponsor_id, sponsor_name;
	
	private int type;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.legislator_list);
		
		Drumbone.apiKey = getResources().getString(R.string.sunlight_api_key);
		Drumbone.baseUrl = getResources().getString(R.string.drumbone_base_url);
		
		Bundle extras = getIntent().getExtras();
		type = extras.getInt("type", BILLS_RECENT);
		
		sponsor_id = extras.getString("sponsor_id");
		sponsor_name = extras.getString("sponsor_name");
		
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
		switch (type) {
		case BILLS_RECENT:
			setTitle("Last " + BILLS + " Introduced Bills");
			break;
		case BILLS_LAW:
			setTitle("Last " + BILLS + " Laws this Session");
			break;
		case BILLS_SPONSOR:
			setTitle("Last " + BILLS + " Bills by " + sponsor_name);
			break;
		}
	}
	
	protected void onListItemClick(ListView parent, View v, int position, long id) {
    	Bill bill = (Bill) parent.getItemAtPosition(position);
    	startActivity(Utils.billIntentExtra(this, bill));
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
				switch (context.type) {
				case BILLS_RECENT:
					return Bill.recentlyIntroduced(BILLS);
				case BILLS_LAW:
					return Bill.recentLaws(BILLS);
				case BILLS_SPONSOR:
					return Bill.recentlySponsored(BILLS, context.sponsor_id);
				default:
					throw new CongressException("Not sure what type of bills to find.");
				}
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
			
			
			String code = Bill.formatCode(bill.code);
			String action;
			Date date;
			switch (type) {
			case BILLS_LAW:
				date = bill.enacted_at;
				action = "became law";
				break;
			case BILLS_RECENT:
			case BILLS_SPONSOR:
			default:
				date = bill.introduced_at;
				action = "was introduced";
				break;
			}
			Spanned byline = Html.fromHtml("<b>" + code + "</b> " + action + ":");
			
			((TextView) view.findViewById(R.id.byline)).setText(byline);
			((TextView) view.findViewById(R.id.date)).setText(new SimpleDateFormat("MMM dd").format(date));
			
			TextView titleView = ((TextView) view.findViewById(R.id.title));
			if (bill.short_title != null) {
				String title = Utils.truncate(bill.short_title, 300);
				titleView.setTextSize(19);
				titleView.setText(title);
			} else { // if (bill.official_title != null)
				String title = Utils.truncate(bill.official_title, 300);
				titleView.setTextSize(16);
				titleView.setText(title);
			} 
			
			view.setTag(bill);
			
			return view;
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