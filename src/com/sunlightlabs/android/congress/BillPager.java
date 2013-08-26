package com.sunlightlabs.android.congress;

import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.sunlightlabs.android.congress.fragments.BillActionFragment;
import com.sunlightlabs.android.congress.fragments.BillInfoFragment;
import com.sunlightlabs.android.congress.fragments.BillLoaderFragment;
import com.sunlightlabs.android.congress.fragments.BillVoteFragment;
import com.sunlightlabs.android.congress.fragments.NewsListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.ActionBarUtils.HasActionMenu;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class BillPager extends FragmentActivity implements HasActionMenu {
	public String bill_id;
	public Bill bill;
	
	public String tab;
	public Database database;
	public Cursor cursor;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		bill_id = extras.getString("bill_id");
		bill = (Bill) extras.getSerializable("bill");
		tab = extras.getString("tab");
		
		// if coming in from a link (in form /bill/:congress/:formatted_code, e.g. "/bill/112/H.R. 2345")
		Uri uri = intent.getData();
		if (uri != null) {
			List<String> segments = uri.getPathSegments();
			if (segments.size() == 3) { // coming in from floor
				String congress = segments.get(1);
				String formattedCode = segments.get(2);
				String code = Bill.normalizeCode(formattedCode);
				bill_id = code + "-" + congress;
			}
		}
		
		setupControls();
		
		if (bill == null)
			BillLoaderFragment.start(this);
		else
			onLoadBill(bill);
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, Bill.formatCode(bill_id));
		
		findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
		findViewById(R.id.pager_container).setVisibility(View.GONE);
		Utils.setLoading(this, R.string.bill_loading);
		
		((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});
	}
	
	private void refresh() {
		this.bill = null;
		Utils.setLoading(this, R.string.bill_loading);
		Utils.showLoading(this);
		BillLoaderFragment.start(this, true);
	}
	
	public void onLoadBill(Bill bill) {
		this.bill = bill;
		
		findViewById(R.id.pager_container).setVisibility(View.VISIBLE);
		findViewById(android.R.id.empty).setVisibility(View.GONE);
		
		setupDatabase();
		setupButtons();
		setupPager();
	}
	
	public void onLoadBill(CongressException exception) {
		if (exception instanceof BillService.BillNotFoundException)
			Utils.showEmpty(this, R.string.bill_not_known_yet);
		else
			Utils.showRefresh(this, R.string.bill_loading_error);
	}
	
	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		
		adapter.add("info", R.string.tab_details, BillInfoFragment.create(bill));
		adapter.add("news", R.string.tab_news, NewsListFragment.forBill(bill));
		adapter.add("history", R.string.tab_history, BillActionFragment.create(bill));
		adapter.add("votes", R.string.tab_votes, BillVoteFragment.create(bill));
		
		if (tab != null) adapter.selectPage(tab);
	}
	
	@SuppressWarnings("deprecation")
	private void setupDatabase() {
		database = new Database(this);
		database.open();
		cursor = database.getBill(bill.id);
		startManagingCursor(cursor);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (database != null && database.isOpen())
			database.close();
	}
	
	public void setupButtons() {
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.star_off, new View.OnClickListener() {
			public void onClick(View v) { 
				toggleDatabaseFavorite(); 
			}
		});
		
		toggleFavoriteStar(cursor.getCount() == 1);
		
		ActionBarUtils.setActionButton(this, R.id.action_2, R.drawable.share, new View.OnClickListener() {
			public void onClick(View v) {
				Analytics.billShare(BillPager.this, bill.id);
	    		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
	    				.putExtra(Intent.EXTRA_TEXT, shareText())
	    				.putExtra(Intent.EXTRA_SUBJECT, shareSubject());
	    		startActivity(Intent.createChooser(intent, "Share bill via:"));
			}
		});
		
		ActionBarUtils.setActionMenu(this, R.menu.bill);
	}
	
	public String shareText() {
		return bill.sunlightShortUrl();
	}
	
	public String shareSubject() {
		return bill.formatCode(bill.id);
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
		if (bill == null) return; // safety valve (only matters on pre-4.0 devices)
		
		switch(item.getItemId()) {
    	case R.id.text:
    		Analytics.billText(this, bill.id);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bill.bestFullTextUrl())));
    		break;
    	case R.id.govtrack:
    		Analytics.billGovTrack(this, bill.id);
    		if (bill.urls != null && bill.urls.containsKey("govtrack"))
    			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bill.urls.get("govtrack"))));
    		break;
    	case R.id.opencongress:
    		Analytics.billOpenCongress(this, bill.id);
    		if (bill.urls != null && bill.urls.containsKey("opencongress"))
    			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bill.urls.get("opencongress"))));
    		break;
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