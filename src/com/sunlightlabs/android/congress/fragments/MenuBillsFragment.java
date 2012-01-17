package com.sunlightlabs.android.congress.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;

public class MenuBillsFragment extends ListFragment {
	
	private Database database;
	private Cursor cursor;
	
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
	
	
	public void setupControls() {
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
		Bill bill = (Bill) view.getTag();
		startActivity(Utils.billLoadIntent(bill.id, bill.code));
	}
	
	class FavoriteBillsAdapter extends CursorAdapter {
	
		public FavoriteBillsAdapter(Context context, Cursor cursor) {
			super(context, cursor);
		}
	
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Bill bill = Database.loadBill(cursor);
			
			((TextView) view.findViewById(R.id.code)).setText(Bill.formatCode(bill.code));
			TextView titleView = (TextView) view.findViewById(R.id.title);
			
			String title;
			if (bill.short_title != null && !bill.short_title.equals("")) {
				title = bill.short_title;
				titleView.setTextSize(16);
			} else if (bill.official_title != null && !bill.official_title.equals("")) {
				title = bill.official_title;
				titleView.setTextSize(14);
			} else {
				title = getResources().getString(R.string.bill_no_title);
				titleView.setTextSize(16);
			}
			
			titleView.setText(Utils.truncate(title, 140));
			
			view.setTag(bill);
		}
	
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = LayoutInflater.from(context).inflate(R.layout.favorite_bill, null);
			bindView(view, context, cursor);
			return view;
		}
			
	}
}