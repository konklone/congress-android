package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.sunlightlabs.android.congress.fragments.BillListFragment;
import com.sunlightlabs.android.congress.fragments.MenuBillsFragment;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;

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
	}
	
}