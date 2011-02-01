package com.sunlightlabs.android.congress;

import java.util.regex.Pattern;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;

public class BillTabs extends TabActivity {
	private Bill bill;
	private String tab;

	private Database database;
	private Cursor cursor;

	private ImageView star;
	
	private GoogleAnalyticsTracker tracker;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bill);
		
		Bundle extras = getIntent().getExtras();
		bill = (Bill) extras.getSerializable("bill");
		tab = extras.getString("tab");
		
		setupControls();
		setupTabs();
		
		if (firstTimeLoadingStar())
        	Utils.alert(this, R.string.first_time_loading_star);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
		Analytics.stop(tracker);
	}

	public void setupControls() {
		database = new Database(this);
		database.open();
		cursor = database.getBill(bill.id);
		startManagingCursor(cursor);
		
		star = (ImageView) findViewById(R.id.favorite);
		star.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggleDatabaseFavorite();
			}
		});

		toggleFavoriteStar(cursor.getCount() == 1);
		
		((TextView) findViewById(R.id.title_text)).setText(Bill.formatCode(bill.code));
		
		View share = findViewById(R.id.share);
		share.setVisibility(View.VISIBLE);
		share.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
	    		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText());
	    		startActivity(Intent.createChooser(intent, "Share bill via:"));
			}
		});
		
		tracker = Analytics.start(this);
	}
	
	public String shareText() {
		String url = Bill.thomasUrl(bill.bill_type, bill.number, bill.session);
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
			if (database.removeBill(id) != 0) {
				toggleFavoriteStar(false);
				Analytics.removeFavoriteBill(this, tracker, id);
			} else
				Utils.alert(this, "Problem unstarring bill.");
		} else {
			if (database.addBill(bill) != -1) {
				toggleFavoriteStar(true);
				Analytics.addFavoriteBill(this, tracker, id);
				
				if (!Utils.hasShownFavoritesMessage(this)) {
					Utils.alert(this, R.string.bill_favorites_message);
					Utils.markShownFavoritesMessage(this);
				}
			} else
				Utils.alert(this, "Problem starring bill.");
		}
	}
	
	public boolean firstTimeLoadingStar() {
		if (Utils.getBooleanPreference(this, "first_time_loading_star", true)) {
			Utils.setBooleanPreference(this, "first_time_loading_star", false);
			return true;
		}
		return false;
	}

	public void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		
		Utils.addTab(this, tabHost, "info", detailsIntent(), getString(R.string.tab_details), res.getDrawable(R.drawable.tab_profile));
		Utils.addTab(this, tabHost, "news", newsIntent(), getString(R.string.tab_news), res.getDrawable(R.drawable.tab_news));
		Utils.addTab(this, tabHost, "history", historyIntent(), getString(R.string.tab_history), res.getDrawable(R.drawable.tab_history));
		
		if (bill.last_passage_vote_at != null && bill.last_passage_vote_at.getTime() > 0)
			Utils.addTab(this, tabHost, "votes", votesIntent(), getString(R.string.tab_votes), res.getDrawable(R.drawable.tab_video));
		
		if (tab != null) 
			tabHost.setCurrentTabByTag(tab);
		else
			tabHost.setCurrentTabByTag("info");
	}
	
	
	public Intent detailsIntent() {
		return Utils.billIntent(this, BillInfo.class, bill);
	}
	
	public Intent newsIntent() {
		return new Intent(this, NewsList.class)
			.putExtra("searchTerm", searchTermFor(bill))
			.putExtra("subscriptionId", bill.id)
			.putExtra("subscriptionName", Subscriber.notificationName(bill))
			.putExtra("subscriptionClass", "NewsBillSubscriber");
	}
	
	public Intent historyIntent() {
		return new Intent(this, BillHistory.class).putExtra("bill", bill);
	}
	
	public Intent votesIntent() {
		return new Intent(this, BillVotes.class).putExtra("bill", bill);
	}
	
	
	/**
	 * Regex for finding bills that end in "of 2009" or the like:
	 *   * \s+   = one or more spaces (or other whitespace)
	 *   * of     = "of"
	 *   * \s+   = one or more spaces (or other whitespace)
	 *   * \d{4} = 4 digits in a row (we'll need to update this to {5} in late 9999)
	 *   * \s*   = zero or more spaces (probably unnecessary)
	 *   * $      = end of line
	 */
	private static Pattern NEWS_SEARCH_REGEX = Pattern.compile("\\s+of\\s+\\d{4}\\s*$", Pattern.CASE_INSENSITIVE);
	
	// for news searching, don't use legislator.titledName() because we don't want to use the name_suffix
	private static String searchTermFor(Bill bill) {
    	if (bill.short_title != null && !bill.short_title.equals(""))
    		return "\"" + NEWS_SEARCH_REGEX.matcher(bill.short_title).replaceFirst("") + "\" OR \"" + Bill.formatCodeShort(bill.code) + "\"";
    	else
    		return "\"" + Bill.formatCodeShort(bill.code) + "\"";
    }
}
