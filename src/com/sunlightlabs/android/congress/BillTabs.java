package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;

public class BillTabs extends TabActivity {
	private Bill bill;
	
	private Database database;
	private Cursor cursor;

	ImageView star;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bill);
		
		bill = (Bill) getIntent().getExtras().getSerializable("bill");
		
		database = new Database(this);
		database.open();
		cursor = database.getBill(bill.id);
		startManagingCursor(cursor);

		setupControls();
		setupTabs();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}

	public void setupControls() {
		star = (ImageView) findViewById(R.id.favorite);
		star.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggleDatabaseFavorite();
			}
		});
		toggleFavoriteStar(cursor.getCount() == 1);

		((TextView) findViewById(R.id.title_text)).setText(Bill.formatCode(bill.code));
	}
	
	private void toggleFavoriteStar(boolean enabled) {
		if (enabled)
			star.setImageResource(R.drawable.star_on);
		else
			star.setImageResource(R.drawable.star_off);
	}

	private void toggleDatabaseFavorite() {
		String id = bill.id;
		cursor.requery();
		
		if (cursor.getCount() == 1) {
			if (database.removeBill(id) != 0)
				toggleFavoriteStar(false);
		} else {
			if (database.addBill(bill) != -1) {
				toggleFavoriteStar(true);
				
				if (!Utils.hasShownFavoritesMessage(this)) {
					Utils.alert(this, R.string.bill_favorites_message);
					Utils.markShownFavoritesMessage(this);
				}
			}
		}
	}

	public void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		
		Utils.addTab(this, tabHost, "info_tab", "Details", detailsIntent(), res.getDrawable(R.drawable.tab_profile));
		Utils.addTab(this, tabHost, "history_tab", "History", historyIntent(), res.getDrawable(R.drawable.tab_news));
		
		if (bill.last_vote_at != null && bill.last_vote_at.getTime() > 0)
			Utils.addTab(this, tabHost, "voted_tab", "Votes", votesIntent(), res.getDrawable(R.drawable.tab_video));
		
		tabHost.setCurrentTab(0);
	}
	
	
	public Intent detailsIntent() {
		return Utils.billIntent(this, BillInfo.class, bill);
	}
	
	public Intent historyIntent() {
		return new Intent(this, BillHistory.class).putExtra("id", bill.id);
	}
	
	public Intent votesIntent() {
		return new Intent(this, BillVotes.class).putExtra("id", bill.id);
	}
}
