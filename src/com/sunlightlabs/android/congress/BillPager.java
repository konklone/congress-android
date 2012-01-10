package com.sunlightlabs.android.congress;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.BillActionFragment;
import com.sunlightlabs.android.congress.fragments.BillInfoFragment;
import com.sunlightlabs.android.congress.fragments.BillVoteFragment;
import com.sunlightlabs.android.congress.fragments.NewsListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.ActionBarUtils.HasActionMenu;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;

public class BillPager extends FragmentActivity implements HasActionMenu {
	private Bill bill;
	private String tab;
	private Database database;
	private Cursor cursor;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Bundle extras = getIntent().getExtras();
		bill = (Bill) extras.getSerializable("bill");
		tab = extras.getString("tab");
		
		Analytics.track(this, "/bill?bill_id=" + bill.id);
		
		setupDatabase();
		setupControls();
		setupPager();	
	}
	
	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		
		adapter.add("info", R.string.tab_details, BillInfoFragment.create(bill));
		adapter.add("news", R.string.tab_news, NewsListFragment.forBill(bill));
		adapter.add("history", R.string.tab_history, BillActionFragment.create(bill));
		adapter.add("votes", R.string.tab_votes, BillVoteFragment.create(bill));
		
		if (tab != null) adapter.selectPage(tab);
	}
	
	private void setupDatabase() {
		database = new Database(this);
		database.open();
		cursor = database.getBill(bill.id);
		startManagingCursor(cursor);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, Bill.formatCode(bill.code));
		
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.star_off, new View.OnClickListener() {
			public void onClick(View v) { 
				toggleDatabaseFavorite(); 
			}
		});
		
		toggleFavoriteStar(cursor.getCount() == 1);
		
		ActionBarUtils.setActionButton(this, R.id.action_2, R.drawable.share, new View.OnClickListener() {
			public void onClick(View v) {
				Analytics.billShare(BillPager.this, bill.id);
	    		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText());
	    		startActivity(Intent.createChooser(intent, "Share bill via:"));
			}
		});
		
		ActionBarUtils.setActionMenu(this, R.menu.legislator);
	}
	
	public String shareText() {
		return Bill.govTrackUrl(bill.bill_type, bill.number, bill.session);
	}

	private void toggleFavoriteStar(boolean enabled) {
		if (enabled)
			ActionBarUtils.setActionIcon(this, R.id.action_1, R.drawable.star_on);
		else
			ActionBarUtils.setActionIcon(this, R.id.action_1, R.drawable.star_off);
	}

	private void toggleDatabaseFavorite() {
		String id = bill.id;
		cursor.requery();
		if (cursor.getCount() == 1) {
			if (database.removeBill(id) != 0) {
				toggleFavoriteStar(false);
				Analytics.removeFavoriteBill(this, id);
			} else
				Utils.alert(this, "Problem unstarring bill.");
		} else {
			if (database.addBill(bill) != -1) {
				toggleFavoriteStar(true);
				Analytics.addFavoriteBill(this, id);
			} else
				Utils.alert(this, "Problem starring bill.");
		}
	}
	
	@Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    super.onCreateOptionsMenu(menu); 
	    getMenuInflater().inflate(R.menu.bill, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	menuSelected(item);
    	return true;
    }
	
	@Override
	public void menuSelected(MenuItem item) {
		switch(item.getItemId()) {
    	case R.id.thomas:
    		Analytics.billThomas(this, bill.id);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.thomasUrl(bill.bill_type, bill.number, bill.session))));
    		break;
    	case R.id.govtrack:
    		Analytics.billGovTrack(this, bill.id);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.govTrackUrl(bill.bill_type, bill.number, bill.session))));
    		break;
    	case R.id.opencongress:
    		Analytics.billOpenCongress(this, bill.id);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Bill.openCongressUrl(bill.bill_type, bill.number, bill.session))));
    		break;
    	}
	}
}