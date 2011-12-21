package com.sunlightlabs.android.congress.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.BillList;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.fragments.MenuBillsFragment.FavoriteBillsAdapter.FavoriteBillWrapper;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;

public class MenuBillsFragment extends ListFragment {
	
	private Database database;
	private Cursor cursor;
	private EditText searchField;
	
	public static MenuBillsFragment newInstance() {
		MenuBillsFragment frag = new MenuBillsFragment();
		frag.setRetainInstance(true);
		return frag;
	}
	
	public MenuBillsFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setupDatabase();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.menu_bills, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
	}
	
	public void setupDatabase() {
		database = new Database(getActivity());
		database.open();
		
		cursor = database.getBills();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (cursor != null) {
			cursor.requery();
			displayFavorites();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		database.close();
	}
	
	private void search() {
		String query = searchField.getText().toString().trim();
		
		if (!query.equals(""))
			searchFor(query);
	}

	// query guaranteed to be non-null and blank
	public void searchFor(String query) {
		// if it's a bill code, do code search
		String code = Bill.normalizeCode(query);
		if (Bill.isCode(code)) {
			startActivity(new Intent(getActivity(), BillList.class)
				.putExtra("type", BillList.BILLS_CODE)
				.putExtra("code", code)
			);
		}
		
		// else, do generic search
		else {
			startActivity(new Intent(getActivity(), BillList.class)
				.putExtra("type", BillList.BILLS_SEARCH)
				.putExtra("query", query)
			);
		}
	}
	
	public void setupControls() {
		//((TextView) getView().findViewById(R.id.following_header)).setText(R.string.menu_bills_favorite);
		
		searchField = (EditText) getView().findViewById(R.id.bill_query);
		getView().findViewById(R.id.bill_search).setOnClickListener(new View.OnClickListener() {
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
		
		displayFavorites();
	}
	
	public void displayFavorites() {
		if (cursor != null && cursor.getCount() > 0) {
			setListAdapter(new FavoriteBillsAdapter(getActivity(), cursor));
			getView().findViewById(R.id.menu_bills_no_favorites).setVisibility(View.GONE);
			getListView().setVisibility(View.VISIBLE);
		} else {
			getView().findViewById(R.id.menu_bills_no_favorites).setVisibility(View.VISIBLE);
			getListView().setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onListItemClick(ListView lv, View view, int position, long id) {
		Bill bill = ((FavoriteBillWrapper) view.getTag()).bill;
		startActivity(Utils.billLoadIntent(bill.id, bill.code));
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
}