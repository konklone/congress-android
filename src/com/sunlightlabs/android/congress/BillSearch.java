package com.sunlightlabs.android.congress;

import android.app.SearchManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.BillListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.congress.models.Bill;

public class BillSearch extends FragmentActivity {
	
	String query;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		query = getIntent().getStringExtra(SearchManager.QUERY).trim();
	    
		setupPager();
	}
	
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		
		String code = Bill.normalizeCode(query);
		if (Bill.isCode(code)) {
			Analytics.track(this, "/bills/code");
			
			ActionBarUtils.setTitle(this, Bill.formatCode(code));
			adapter.add("bills_code", "Not seen", BillListFragment.forCode(code));
			findViewById(R.id.pager_titles).setVisibility(View.GONE);
		} else {
			Analytics.track(this, "/bills/search");
			
			ActionBarUtils.setTitle(this, "Bills matching \"" + query + "\"");
			ActionBarUtils.setTitleSize(this, 16);
			adapter.add("bills_recent", R.string.search_bills_recent, BillListFragment.forSearch(query, BillListFragment.BILLS_SEARCH_NEWEST));
			adapter.add("bills_relevant", R.string.search_bills_relevant, BillListFragment.forSearch(query, BillListFragment.BILLS_SEARCH_RELEVANT));
		}
	}
	
}