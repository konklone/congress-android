package com.sunlightlabs.android.congress.utils;


import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class FavBillsAdapter extends CursorAdapter {

	public FavBillsAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		try {
			((FavBillWrapper) view.getTag()).populateFrom(cursor);
		} catch(CongressException e) {
			Utils.alert(context, R.string.menu_favorite_bill_error);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View row = LayoutInflater.from(context).inflate(R.layout.favorite_bill, null);
		FavBillWrapper wrapper = new FavBillWrapper(row);
		
		try {
			wrapper.populateFrom(cursor);
		} catch(CongressException e) {
			Utils.alert(context, R.string.menu_favorite_bill_error);
		}
		
		row.setTag(wrapper);
		return row;
	}
	
	public class FavBillWrapper {
		private View row;
		private TextView code, title;
		
		public Bill bill;

		public FavBillWrapper(View row) {
			this.row = row;
		}
		
		public void populateFrom(Cursor c) throws CongressException {
			bill = Bill.fromCursor(c);
			getCode().setText(Bill.formatCode(bill.code));
			String title;
			if (bill.short_title != null && !bill.short_title.equals(""))
				title = bill.short_title;
			else
				title = bill.official_title;
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
