package com.sunlightlabs.android.congress.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.util.Log;
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
		FavBillWrapper wrapper = (FavBillWrapper) view.getTag();
		wrapper.populateFrom(cursor);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View row = inflater.inflate(R.layout.bill_item, null);
		FavBillWrapper wrapper = new FavBillWrapper(row);
		row.setTag(wrapper);
		wrapper.populateFrom(cursor);
		return row;
	}
	
	public class FavBillWrapper {
		private View row;
		private TextView byline, date, title;
		private Bill bill;
		
		public FavBillWrapper(View row) {
			this.row = row;
		}
		public TextView getByLine() {
			return byline == null ? byline = (TextView) row.findViewById(R.id.byline) : byline;
		}
		public TextView getDate() {
			return date == null ? date = (TextView) row.findViewById(R.id.date) : date;
		}
		public TextView getTitle() {
			return title == null ? (TextView) row.findViewById(R.id.title) : title;
		}

		public Bill getBill() {
			return bill;
		}

		public void populateFrom(Cursor c) {
			try {
				bill = Bill.fromCursor(c);
				getByLine().setText(Html.fromHtml("<b>" + Bill.formatCode(bill.code) + "</b> "));

				Date date = bill.introduced_at;
				if (date != null) {
					SimpleDateFormat format = null;
					if (date.getYear() == new Date().getYear())
						format = new SimpleDateFormat("MMM dd");
					else
						format = new SimpleDateFormat("MMM dd, yyyy");
					getDate().setText(format.format(date));
				} else
					getDate().setText("");

				if (bill.short_title != null) {
					String title = Utils.truncate(bill.short_title, 200);
					getTitle().setTextSize(18);
					getTitle().setText(title);
				} else {
					String title = Utils.truncate(bill.official_title, 200);
					getTitle().setTextSize(16);
					getTitle().setText(title);
				}
			} catch (CongressException e) {
				Log.e(this.getClass().getName(), "Could not populate a Bill from a cursor.", e);
			}
		}
	}
	
	
}
