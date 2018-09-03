package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.os.Bundle;

import com.sunlightlabs.android.congress.fragments.BillListFragment;
import com.sunlightlabs.android.congress.fragments.MenuBillsFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class MenuBills extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Analytics.init(this);
		setContentView(R.layout.pager_titled);
		
		setupControls();
		setupPager();
	}
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("bills_menu", R.string.menu_bills_menu, MenuBillsFragment.newInstance());
		adapter.add("bills_active", R.string.menu_bills_active, BillListFragment.forActive());
		adapter.add("bills_law", R.string.menu_bills_law, BillListFragment.forLaw());
		adapter.add("bills_new", R.string.menu_bills_recent, BillListFragment.forAll());
		
		String tab = getIntent().getStringExtra("tab");
		
		if (tab != null && tab.equals("bills_new"))
			adapter.selectPage(3);
		else if (tab != null && tab.equals("bills_law"))
			adapter.selectPage(2);
		else if (tab != null && tab.equals("bills_active"))
			adapter.selectPage(1);
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, R.string.menu_main_bills);

		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.search, v -> onSearchRequested());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Analytics.start(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Analytics.stop(this);
	}
}