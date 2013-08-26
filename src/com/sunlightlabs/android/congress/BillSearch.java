package com.sunlightlabs.android.congress;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.BillListFragment;
import com.sunlightlabs.android.congress.providers.SuggestionsProvider;
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
		setupControls();
	}
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		
		String code = Bill.normalizeCode(query);
		if (Bill.isCode(code)) {
			// store the formatted code as the search suggestion
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this, SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
			
			String bill_type;
			int number;
			
			Pattern pattern = Pattern.compile("^([a-z]+)(\\d+)$");
			Matcher matcher = pattern.matcher(code);
			matcher.find(); // isCode should guarantee this
			bill_type = matcher.group(1);
			number = Integer.valueOf(matcher.group(2));
			
			String formattedCode = Bill.formatCode(bill_type, number);
	        suggestions.saveRecentQuery(formattedCode, null);
			
			ActionBarUtils.setTitle(this, formattedCode);
			adapter.add("bills_code", "Not seen", BillListFragment.forCode(bill_type, number));
			findViewById(R.id.pager_titles).setVisibility(View.GONE);
		} else {
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this, SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
	        suggestions.saveRecentQuery(query, null);
			
			ActionBarUtils.setTitle(this, "Bills matching \"" + query + "\"", new Intent(this, MenuBills.class));
			ActionBarUtils.setTitleSize(this, 16);
			adapter.add("bills_recent", R.string.search_bills_recent, BillListFragment.forSearch(query, BillListFragment.BILLS_SEARCH_NEWEST));
			adapter.add("bills_relevant", R.string.search_bills_relevant, BillListFragment.forSearch(query, BillListFragment.BILLS_SEARCH_RELEVANT));
		}
	}
	
	public void setupControls() {
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.search, new View.OnClickListener() {
			public void onClick(View v) { 
				onSearchRequested();
			}
		});
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