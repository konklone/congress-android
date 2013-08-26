package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.BillListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class BillSponsor extends FragmentActivity {
	
	Legislator sponsor;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		sponsor = (Legislator) getIntent().getSerializableExtra("legislator");
		
		setupControls();
		setupPager();
	}
	
	public void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		findViewById(R.id.pager_titles).setVisibility(View.GONE);
		
		adapter.add("bills_sponsor", "Not seen", BillListFragment.forSponsor(sponsor));
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, "Latest Bills by\n" + sponsor.titledName(), Utils.legislatorIntent(this, sponsor));
		ActionBarUtils.setTitleSize(this, 16);
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