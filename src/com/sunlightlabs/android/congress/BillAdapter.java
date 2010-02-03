package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.Legislator;

public class BillAdapter extends ArrayAdapter<Bill> {
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
		((TextView) view.findViewById(R.id.title)).setText(Utils.truncate(bill.getTitle(), 130));
		
		view.setTag("bill-" + bill.id);
		
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