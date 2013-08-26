package com.sunlightlabs.android.congress;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.sunlightlabs.android.congress.fragments.LegislatorLoaderFragment;
import com.sunlightlabs.android.congress.fragments.LegislatorProfileFragment;
import com.sunlightlabs.android.congress.fragments.NewsListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.ActionBarUtils.HasActionMenu;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorPager extends FragmentActivity implements HasActionMenu {
	public String bioguide_id;
	public Legislator legislator;
	public String tab;
	
	public Database database;
	public Cursor cursor;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Bundle extras = getIntent().getExtras();
		bioguide_id = extras.getString("bioguide_id");
		legislator = (Legislator) extras.getSerializable("legislator");
		tab = extras.getString("tab");
		
		setupControls();
		
		if (legislator == null)
			LegislatorLoaderFragment.start(this);
		else
			onLoadLegislator(legislator);
	}
	
	public void onLoadLegislator(Legislator legislator) {
		this.legislator = legislator;
		
		findViewById(R.id.pager_container).setVisibility(View.VISIBLE);
		findViewById(android.R.id.empty).setVisibility(View.GONE);
		
		setupDatabase();
		setupButtons();
		setupPager();	
	}
	
	public void onLoadLegislator(CongressException exception) {
		Utils.showRefresh(this, R.string.legislator_loading_error);
	}
	
	private void refresh() {
		this.legislator = null;
		Utils.setLoading(this, R.string.legislator_loading);
		Utils.showLoading(this);
		LegislatorLoaderFragment.start(this, true);
	}
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, R.string.app_name);
		
		findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
		findViewById(R.id.pager_container).setVisibility(View.GONE);
		Utils.setLoading(this, R.string.legislator_loading);
		
		((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});
	}
	
	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("info", R.string.tab_profile, LegislatorProfileFragment.create(legislator));
		adapter.add("news", R.string.tab_news, NewsListFragment.forLegislator(legislator));
		
		if (tab != null) adapter.selectPage(tab);
	}
	
	private void setupDatabase() {
		database = new Database(this);
		database.open();
		cursor = database.getLegislator(legislator.bioguide_id);
		startManagingCursor(cursor);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (database != null && database.isOpen())
			database.close();
	}
	
	public void setupButtons() {
		String titledName = legislator.titledName();
		ActionBarUtils.setTitle(this, titledName, new Intent(this, MenuLegislators.class));
		if (titledName.length() >= 23)
			ActionBarUtils.setTitleSize(this, 16);
		
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.star_off, new View.OnClickListener() {
			public void onClick(View v) { 
				toggleDatabaseFavorite(); 
			}
		});
		
		toggleFavoriteStar(cursor.getCount() == 1);
		
		ActionBarUtils.setActionMenu(this, R.menu.legislator);
	}

	private void toggleFavoriteStar(boolean enabled) {
		if (enabled)
			ActionBarUtils.setActionIcon(this, R.id.action_1, R.drawable.star_on);
		else
			ActionBarUtils.setActionIcon(this, R.id.action_1, R.drawable.star_off);
	}

	private void toggleDatabaseFavorite() {
		String id = legislator.bioguide_id;
		cursor.requery();
		if (cursor.getCount() == 1) {
			if (database.removeLegislator(id) != 0) {
				toggleFavoriteStar(false);
				Analytics.removeFavoriteLegislator(this, id);
			} else
				Utils.alert(this, "Problem unstarring legislator.");
		} else {
			if (database.addLegislator(legislator) != -1) {
				toggleFavoriteStar(true);
				Analytics.addFavoriteLegislator(this, id);
			} else
				Utils.alert(this, "Problem starring legislator.");
		}
	}
	
	@Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
		super.onCreateOptionsMenu(menu); 
	    getMenuInflater().inflate(R.menu.legislator, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	menuSelected(item);
    	return true;
    }
	
    @Override
    public void menuSelected(MenuItem item) {
    	if (legislator == null) return; // safety valve (only matters on pre-4.0 devices)
    	
        switch(item.getItemId()) {
            case R.id.addcontact:
            	Analytics.legislatorContacts(this, legislator.bioguide_id);
                openContactAdd();
                break;
            case R.id.govtrack:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.govTrackUrl(legislator.govtrack_id))));
    		break;
            case R.id.opencongress:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.openCongressUrl(legislator.govtrack_id))));
    		break;
            case R.id.bioguide:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.bioguideUrl(legislator.bioguide_id))));
    		break;
        }
    }

    private void openContactAdd() {
        Intent i = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
        i.putExtra(ContactsContract.Intents.Insert.NAME,  this.legislator.getName());
        if (!TextUtils.isEmpty(this.legislator.phone)) {
            i.putExtra(ContactsContract.Intents.Insert.PHONE, this.legislator.phone);
            i.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
        }
        i.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, this.legislator.fullTitle());
        startActivity(i);
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