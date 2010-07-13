package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
	Button share;

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
		
		share = (Button) findViewById(R.id.share);
		share.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
	    		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText());
	    		startActivity(Intent.createChooser(intent, "Share bill"));
			}
		});
	}
	
	public String shareText() {
		String url = Bill.thomasUrl(bill.type, bill.number, bill.session);
		String short_title = bill.short_title;
		if (short_title != null && !short_title.equals(""))
			return "Check out the " + short_title + " on THOMAS: " + url;
		else
			return "Check out the bill " + Bill.formatCode(bill.code) + " on THOMAS: " + url;
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
		Utils.addTab(this, tabHost, "news_tab", "News", newsIntent(), res.getDrawable(R.drawable.tab_news));
		Utils.addTab(this, tabHost, "history_tab", "History", historyIntent(), res.getDrawable(R.drawable.tab_history));
		
		if (bill.last_vote_at != null && bill.last_vote_at.getTime() > 0)
			Utils.addTab(this, tabHost, "voted_tab", "Votes", votesIntent(), res.getDrawable(R.drawable.tab_video));
		
		tabHost.setCurrentTab(0);
	}
	
	
	public Intent detailsIntent() {
		return Utils.billIntent(this, BillInfo.class, bill);
	}
	
	public Intent newsIntent() {
		return new Intent(this, NewsList.class).putExtra("searchTerm", searchTermFor(bill));
	}
	
	public Intent historyIntent() {
		return new Intent(this, BillHistory.class).putExtra("id", bill.id);
	}
	
	public Intent votesIntent() {
		return new Intent(this, BillVotes.class).putExtra("id", bill.id);
	}
	
	// for news searching, don't use legislator.titledName() because we don't want to use the name_suffix
	private static String searchTermFor(Bill bill) {
    	if (bill.short_title != null && !bill.short_title.equals(""))
    		return bill.short_title;
    	else
    		return Bill.formatCodeShort(bill.code);
    }
}