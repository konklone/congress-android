package com.sunlightlabs.android.congress;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.fragments.BillListFragment;
import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.fragments.LegislatorLoaderFragment;
import com.sunlightlabs.android.congress.fragments.RollListFragment;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.ActionBarUtils.HasActionMenu;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorPager extends Activity implements HasActionMenu, LoadPhotoTask.LoadsPhoto {
	public String bioguide_id;
	public Legislator legislator;
	public String tab;

    private Drawable avatar;
	
	public Database database;
	public Cursor cursor;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_legislator);

        Analytics.init(this);
        Utils.setupAPI(this);
		
		Bundle extras = getIntent().getExtras();
		bioguide_id = extras.getString("bioguide_id");
		legislator = (Legislator) extras.getSerializable("legislator");
		tab = extras.getString("tab");
		
		setupControls();

        // currently unused - we always use LegislatorLoaderFragment to get in here
		if (legislator == null)
			LegislatorLoaderFragment.start(this);

        // in practice, always the route
        else
			onLoadLegislator(legislator);
	}
	
	public void onLoadLegislator(Legislator legislator) {
		this.legislator = legislator;
		
		findViewById(R.id.pager_container).setVisibility(View.VISIBLE);
		findViewById(android.R.id.empty).setVisibility(View.GONE);
		
		setupDatabase();
		setupProfile();
		setupPager();

        if (avatar != null)
            displayAvatar();
        else
            loadPhoto();
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
		
		findViewById(R.id.refresh).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});
    }
	
	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("votes", R.string.tab_votes, RollListFragment.forLegislator(legislator));
		adapter.add("bills", R.string.tab_bills, BillListFragment.forSponsor(legislator));
		adapter.add("committees", R.string.committees, CommitteeListFragment.forLegislator(legislator));
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
	
	public void setupProfile() {
		String titledName = legislator.titledName();
		ActionBarUtils.setTitle(this, titledName, new Intent(this, MenuLegislators.class));
        if (titledName.length() >= 23)
            ActionBarUtils.setTitleSize(this, 16);
		
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.star_off, new View.OnClickListener() {
			public void onClick(View v) { 
				toggleDatabaseFavorite(); 
			}
		});

        // allow for devices without phones
        if ((legislator.phone != null) && (!legislator.phone.equals(""))) {
            ActionBarUtils.setActionButton(this, R.id.call, android.R.drawable.ic_menu_call, new View.OnClickListener() {
                public void onClick(View v) {
                    callOffice();
                }
            });
        }
		
		toggleFavoriteStar(cursor.getCount() == 1);
		
		ActionBarUtils.setActionMenu(this, R.menu.legislator);

        if (!legislator.in_office)
            findViewById(R.id.out_of_office_text).setVisibility(View.VISIBLE);

        String party = partyName(legislator.party);
        String state = Utils.stateCodeToName(this, legislator.state);
        String description;
        if (legislator.chamber.equals("senate"))
            description = party + " from " + state;
        else if (legislator.at_large)
            description = party + " from " + state;
        else
            description = party + " from " + state + "-" + legislator.district;

        ((TextView) findViewById(R.id.profile_state_party)).setText(description);

        TextView officeView = (TextView) findViewById(R.id.profile_office);
        if (legislator.office != null && !legislator.office.equals(""))
            officeView.setText(officeName(legislator.office));
        else
            officeView.setVisibility(View.GONE);
    }

    public void loadPhoto() {
        new LoadPhotoTask(this, LegislatorImage.PIC_LARGE).execute(legislator.bioguide_id);
    }

    @Override
    public void onLoadPhoto(Drawable avatar, Object tag) {
        if (avatar == null) {
            Resources resources = getResources();
            if (resources != null)
                avatar = resources.getDrawable(R.drawable.person);
        }
        this.avatar = avatar;
        displayAvatar();
    }

    public void displayAvatar() {
        ActionBarUtils.setTitleIcon(this, avatar);
    }

    public void callOffice() {
        Analytics.legislatorCall(this, legislator.bioguide_id);

        // if user gave us permission to directly initiate calls, do so
        if (Utils.checkPermission(this, Manifest.permission.CALL_PHONE))
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel://" + legislator.phone)));
        // otherwise, open up the dialer with the number ready to go (needs no permission)
        else
            startActivity(new Intent(Intent.ACTION_DIAL));
    }

    public void visit(String url, String social) {
        Analytics.legislatorWebsite(this, legislator.bioguide_id, social);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void socialButton(int id, final String url, final String network) {
        View view = findViewById(id);
        if (url != null && !url.equals("")) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    visit(url, network);
                }
            });
        } else
            view.setVisibility(View.GONE);
    }

    public static String partyName(String code) {
        if (code.equals("D"))
            return "Democrat";
        if (code.equals("R"))
            return "Republican";
        if (code.equals("I"))
            return "Independent";
        else
            return "";
    }

    public static String officeName(String office) {
        return office.replaceAll("(?:House|Senate)? ?(?:Office)? ?Building", "").trim();
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
            case R.id.bioguide:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.bioguideUrl(legislator.bioguide_id))));
    		break;
            case R.id.visit_website:
                visit(legislator.website, Analytics.LEGISLATOR_WEBSITE);
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

	@Override
    public Context getContext() {
        return this;
    }
}