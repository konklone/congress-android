package com.sunlightlabs.android.congress;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sunlightlabs.android.congress.fragments.LegislatorListFragment;
import com.sunlightlabs.android.congress.fragments.MenuLegislatorsFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class MenuLegislators extends FragmentActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		setupControls();
		setupPager();
	}
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("legislators_menu", R.string.menu_legislators_following, MenuLegislatorsFragment.newInstance());
		adapter.add("legislators_states", R.string.menu_legislators_state, StatesFragment.newInstance());
		adapter.add("legislators_house", R.string.menu_legislators_house, LegislatorListFragment.forChamber("house"));
		adapter.add("legislators_senate", R.string.menu_legislators_senate, LegislatorListFragment.forChamber("senate"));
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, R.string.menu_main_legislators);
		
		ActionBarUtils.setActionButton(this, R.id.action_2, R.drawable.location, new View.OnClickListener() {
			public void onClick(View v) { 
				startActivity(new Intent(MenuLegislators.this, LegislatorSearch.class).putExtra("location", true));
			}
		});
		
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.search, new View.OnClickListener() {
			public void onClick(View v) { 
				onSearchRequested();
			}
		});
	}
	
	public static class StatesFragment extends ListFragment {
		private String[] stateCodes;
		
		public static StatesFragment newInstance() {
			StatesFragment frag = new StatesFragment();
			frag.setRetainInstance(true);
			return frag;
		}
		
		public StatesFragment() {}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.list_bare, container, false);
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			stateCodes = getResources().getStringArray(R.array.state_codes);
			String[] stateNames = getResources().getStringArray(R.array.state_names);
					
			setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, stateNames));
		}
		
		@Override
		public void onListItemClick(ListView parent, View view, int position, long id) {
			startActivity(new Intent(getActivity(), LegislatorSearch.class).putExtra("state", stateCodes[position]));
		}
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