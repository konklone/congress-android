package com.sunlightlabs.android.congress;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.sunlightlabs.android.congress.fragments.BillListFragment;
import com.sunlightlabs.android.congress.fragments.MenuBillsFragment;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;

public class MenuBills extends FragmentActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Analytics.track(this, "/menu/bills");
		
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("bills_menu", R.string.menu_bills_menu, MenuBillsFragment.newInstance());
		adapter.add("bills_new", R.string.menu_bills_recent, BillListFragment.forRecent());
		adapter.add("bills_law", R.string.menu_bills_law, BillListFragment.forLaws());
		
		String tab = getIntent().getStringExtra("tab");
		if (tab != null) { 
			if (tab.equals("bills_new"))
				adapter.selectPage(1);
			else if (tab.equals("bills_law"))
				adapter.selectPage(2);
		}
	}
	
	public void setupControls() {
		Utils.setTitle(this, R.string.menu_main_bills);
		
		Utils.setActionButton(this, R.id.action_1, R.drawable.ic_btn_search, new View.OnClickListener() {
			public void onClick(View v) { 

			}
		});
	}
	
//	private void search() {
//		String query = searchField.getText().toString().trim();
//		
//		if (!query.equals(""))
//			searchFor(query);
//	}

//	// query guaranteed to be non-null and blank
//	public void searchFor(String query) {
//		// if it's a bill code, do code search
//		String code = Bill.normalizeCode(query);
//		if (Bill.isCode(code)) {
//			startActivity(new Intent(this, BillList.class)
//				.putExtra("type", BillList.BILLS_CODE)
//				.putExtra("code", code)
//			);
//		}
//		
//		// else, do generic search
//		else {
//			startActivity(new Intent(this, BillList.class)
//				.putExtra("type", BillList.BILLS_SEARCH)
//				.putExtra("query", query)
//			);
//		}
//	}
	
}