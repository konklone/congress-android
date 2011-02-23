package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.MenuBills.FavoriteBillsAdapter.FavoriteBillWrapper;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.android.congress.utils.ViewWrapper;
import com.sunlightlabs.congress.models.Bill;

public class MenuBills extends ListActivity {
	public static final int BILLS_LAW = 0;
	public static final int BILLS_RECENT = 1;
	
	public static final int RESULT_BILL_CODE = 4;
	
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;
	
	private MergeAdapter adapter;
	
	private Database database;
	private Cursor cursor;
	
	private EditText searchField;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu_bills);
		
		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder != null) {
			tracked = holder.tracked;
		}
		
		tracker = Analytics.start(this);
		if (!tracked) {
			Analytics.page(this, tracker, "/menu/bills");
			tracked = true;
		}

		setupDatabase();
		setupControls();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new Holder(tracked);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (cursor != null)
			cursor.requery();
		setupControls();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
		Analytics.stop(tracker);
	}
	
	public void setupControls() {
		Utils.setTitle(this, R.string.menu_main_bills);
		Utils.setTitleIcon(this, R.drawable.bills);
		
		LayoutInflater inflater = LayoutInflater.from(this);
		adapter = new MergeAdapter();
		
		List<View> billViews = new ArrayList<View>();
		billViews.add(inflateItem(inflater, R.drawable.bill, R.string.menu_bills_recent, BILLS_RECENT));
		billViews.add(inflateItem(inflater, R.drawable.bill_law, R.string.menu_bills_law, BILLS_LAW));
		adapter.addAdapter(new ViewArrayAdapter(this, billViews));
		
		adapter.addView(Utils.inflateHeader(inflater, R.string.menu_legislators_favorite));
		
		if (cursor != null && cursor.getCount() > 0) {
			adapter.addAdapter(new FavoriteBillsAdapter(this, cursor));
		} else {
			TextView noFavorites = (TextView) inflater.inflate(R.layout.menu_no_favorites, null);
			noFavorites.setText(R.string.menu_bills_no_favorites);
			adapter.addView(noFavorites);
		}
		
		setListAdapter(adapter);
		
		searchField = (EditText) findViewById(R.id.bill_query);
		findViewById(R.id.bill_search).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search();
			}
		});
		
		searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE)
					search();
				return false;
			}
		});
		
		searchField.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					search();
					return true;
				}
				return false;
			}
		});
	}
	
	private void search() {
		String query = searchField.getText().toString().trim();
		
		if (!query.equals(""))
			searchFor(query);
	}
	
	private View inflateItem(LayoutInflater inflater, int icon, int text, Object tag) {
		View item = inflater.inflate(R.layout.menu_item, null);
		((ImageView) item.findViewById(R.id.icon)).setImageResource(icon);
		((TextView) item.findViewById(R.id.text)).setText(text);
		item.setTag(new ViewWrapper(item, tag));
		return item;
	}
	
	public void setupDatabase() {
		database = new Database(this);
		database.open();
		
		cursor = database.getBills();
		startManagingCursor(cursor);
	}
	
	// query guaranteed to be non-null and blank
	public void searchFor(String query) {
		// if it's a bill code, do code search
		String code = Bill.normalizeCode(query);
		if (Bill.isCode(code)) {
			startActivity(new Intent(this, BillList.class)
				.putExtra("type", BillList.BILLS_CODE)
				.putExtra("code", code)
			);
		}
		
		// else, do generic search
		else {
			startActivity(new Intent(this, BillList.class)
				.putExtra("type", BillList.BILLS_SEARCH)
				.putExtra("query", query)
			);
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Object tag = v.getTag();
		if (tag instanceof ViewWrapper) {
			ViewWrapper wrapper = (ViewWrapper) v.getTag();
			int type = ((Integer) wrapper.getTag()).intValue();
			switch (type) {
			case BILLS_RECENT:
				startActivity(new Intent(this, BillList.class).putExtra("type",	BillList.BILLS_RECENT));
				break;
			case BILLS_LAW:
				startActivity(new Intent(this, BillList.class).putExtra("type", BillList.BILLS_LAW));
				break;
			default:
				break;
			}
		}
		else if (tag instanceof FavoriteBillWrapper) {
			Bill bill = ((FavoriteBillWrapper) tag).bill;
			startActivity(Utils.billLoadIntent(bill.id, bill.code));
		}
	}
	
	class FavoriteBillsAdapter extends CursorAdapter {

		public FavoriteBillsAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((FavoriteBillWrapper) view.getTag()).populateFrom(cursor);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = LayoutInflater.from(context).inflate(R.layout.favorite_bill, null);
			FavoriteBillWrapper wrapper = new FavoriteBillWrapper(row);
			
			wrapper.populateFrom(cursor);
			
			row.setTag(wrapper);
			return row;
		}
		
		class FavoriteBillWrapper {
			private View row;
			private TextView code, title;
			
			public Bill bill;

			public FavoriteBillWrapper(View row) {
				this.row = row;
			}
			
			public void populateFrom(Cursor c) {
				bill = Database.loadBill(c);
				getCode().setText(Bill.formatCode(bill.code));
				String title;
				if (bill.short_title != null && !bill.short_title.equals(""))
					title = bill.short_title;
				else if (bill.official_title != null && !bill.official_title.equals(""))
					title = bill.official_title;
				else
					title = getResources().getString(R.string.bill_no_title);
				
				getTitle().setText(Utils.truncate(title, 80));
			}
			
			private TextView getCode() {
				return code == null ? code = (TextView) row.findViewById(R.id.code) : code;
			}
			
			private TextView getTitle() {
				return title == null ? title = (TextView) row.findViewById(R.id.title) : title;
			}
		}
			
	}
	
	private static class Holder {
		boolean tracked;
		
		Holder(boolean tracked) {
			this.tracked = tracked;
		}
	}
}