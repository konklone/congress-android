package com.sunlightlabs.android.congress;

import android.app.ListActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.Bill;

public class BillInfo extends ListActivity {
	private String id, code, title;
	private Time introduced_at;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		code = extras.getString("code");
		title = extras.getString("title");
		introduced_at = new Time();
		introduced_at.set(extras.getLong("introduced_at"));
		
		setupControls();
	}
	
	public void setupControls() {
		LayoutInflater inflater = LayoutInflater.from(this);
		LinearLayout header = (LinearLayout) inflater.inflate(R.layout.bill_header, null);
		((TextView) header.findViewById(R.id.code)).setText(Bill.formatCode(code));
		TextView titleView = (TextView) header.findViewById(R.id.title);
		String truncated = truncateTitle(title);
		titleView.setText(truncated);
		titleView.setTextSize(sizeOfTitle(truncated));
		
		MergeAdapter adapter = new MergeAdapter();
		adapter.addView(header);
		setListAdapter(adapter);
	}
	
	public int sizeOfTitle(String title) {
		int length = title.length();
		if (length <= 100)
			return 18;
		else if (length <= 200)
			return 16;
		else if (length <= 300)
			return 14;
		else if (length <= 400)
			return 12;
		else // should be truncated below this anyhow
			return 12;
	}
	
	public String truncateTitle(String title) {
		if (title.length() > 400)
			return Utils.truncate(title, 400);
		else
			return title;
	}
}