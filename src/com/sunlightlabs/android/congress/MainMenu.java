package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.sunlightlabs.android.congress.MainMenu.FavoriteBillsAdapter.FavoriteBillWrapper;
import com.sunlightlabs.android.congress.MainMenu.FavoriteLegislatorsAdapter.FavoriteLegislatorWrapper;
import com.sunlightlabs.android.congress.notifications.NotificationService;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.AddressUpdater;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.LocationUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.android.congress.utils.ViewWrapper;
import com.sunlightlabs.android.congress.utils.AddressUpdater.AddressUpdateable;
import com.sunlightlabs.android.congress.utils.LocationUtils.LocationListenerTimeout;
import com.sunlightlabs.android.congress.utils.LocationUtils.LocationTimer;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class MainMenu extends ListActivity implements LocationListenerTimeout,
		AddressUpdateable<MainMenu>, LoadPhotoTask.LoadsPhoto {

	public static final int RESULT_ZIP = 1;
	public static final int RESULT_LASTNAME = 2;
	public static final int RESULT_STATE = 3;
	public static final int RESULT_BILL_CODE = 4;
	public static final int RESULT_COMMITTEE = 5;

	private static final int ABOUT = 0;
	private static final int FIRST = 1;
	private static final int CHANGELOG = 2;
	private static final int DONATION = 3;

	public static final int BILLS_LAW = 0;
	public static final int BILLS_RECENT = 1;
	public static final int BILLS_CODE = 2;
	public static final int SEARCH_LOCATION = 3;
	public static final int SEARCH_ZIP = 4;
	public static final int SEARCH_STATE = 5;
	public static final int SEARCH_NAME = 6;
	public static final int SEARCH_COMMITTEE = 7;
	public static final int VOTES_LATEST = 8;
	public static final int VOTES_NOMINATIONS = 9;

	public static final String TAG = "CONGRESS";

	private Location location;
	private LocationTimer timer;
	private String address;
	private AddressUpdater addressUpdater;

	private SearchViewWrapper searchLocationView;
	private ViewArrayAdapter searchLocationAdapter;

	private MergeAdapter adapter;

	private Database database;
	private Cursor peopleCursor, billCursor;

	private HashMap<String, LoadPhotoTask> loadPhotoTasks = new HashMap<String, LoadPhotoTask>();
	private HashMap<String, FavoriteLegislatorWrapper> favoritePeopleWrappers = new HashMap<String, FavoriteLegislatorWrapper>();

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			onTimeout((String) msg.obj);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);

		// open the database to get the favorites
		try {
			database = new Database(this);
			database.open();
			
			billCursor = database.getBills();
			startManagingCursor(billCursor);
			
			peopleCursor = database.getLegislators();
			startManagingCursor(peopleCursor);
		} catch(SQLiteException e) {
			Utils.alert(this, R.string.error_loading_favorites);
			billCursor = null;
			peopleCursor = null;
		}

		MainMenuHolder holder = (MainMenuHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			addressUpdater = holder.addressUpdater;
			location = holder.location;
			address = holder.address;

			loadPhotoTasks = holder.loadPhotoTasks;

			if (loadPhotoTasks != null) {
				Iterator<LoadPhotoTask> iterator = loadPhotoTasks.values().iterator();
				while (iterator.hasNext())
					iterator.next().onScreenLoad(this);
			}
			favoritePeopleWrappers = holder.favoritePeopleWrappers;
		}

		if (addressUpdater != null)
			addressUpdater.onScreenLoad(this);

		setupControls();

		if (firstTime()) {
			newVersion(); // don't need to see the changelog on first install
			showDialog(FIRST);
			setNotificationState(); // initially, all notifications are stopped
		} else if (newVersion())
			showDialog(DONATION);
		
			// temporarily swapped out, this release is to ask for donations
			//showDialog(CHANGELOG);
	}

	static class MainMenuHolder {
		AddressUpdater addressUpdater;
		Location location;
		String address;
		HashMap<String, LoadPhotoTask> loadPhotoTasks;
		HashMap<String, FavoriteLegislatorWrapper> favoritePeopleWrappers;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		MainMenuHolder holder = new MainMenuHolder();
		holder.addressUpdater = addressUpdater;
		holder.location = location;
		holder.address = address;
		holder.loadPhotoTasks = loadPhotoTasks;
		holder.favoritePeopleWrappers = favoritePeopleWrappers;
		return holder;
	}

	@Override
	protected void onStart() {
		super.onStart();
		setupLocation();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (peopleCursor != null)
			peopleCursor.requery();
		if (billCursor != null)
			billCursor.requery();
		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onStop() {
		super.onStop();
		cancelTimer();
		toggleLocationEnabled(true);
		toggleLocationLoading(false);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Object tag = v.getTag();
		if (tag instanceof ViewWrapper) {
			ViewWrapper wrapper = (ViewWrapper) v.getTag();
			int type = ((Integer) wrapper.getTag()).intValue();
			switch (type) {
			case SEARCH_LOCATION:
				searchByLatLong(location.getLatitude(), location.getLongitude(), address);
				break;
			case SEARCH_ZIP:
				getResponse(RESULT_ZIP);
				break;
			case SEARCH_NAME:
				getResponse(RESULT_LASTNAME);
				break;
			case SEARCH_STATE:
				getResponse(RESULT_STATE);
				break;
			case SEARCH_COMMITTEE:
				startActivity(new Intent(this, CommitteeTabs.class));
				break;
			case BILLS_RECENT:
				startActivity(new Intent(this, BillList.class).putExtra("type",	BillList.BILLS_RECENT));
				break;
			case BILLS_LAW:
				startActivity(new Intent(this, BillList.class).putExtra("type", BillList.BILLS_LAW));
				break;
			case BILLS_CODE:
				getResponse(RESULT_BILL_CODE);
				break;
			case VOTES_LATEST:
				startActivity(new Intent(this, RollList.class).putExtra("type", RollList.ROLLS_LATEST));
				break;
			case VOTES_NOMINATIONS:
				startActivity(new Intent(this, RollList.class).putExtra("type", RollList.ROLLS_NOMINATIONS));
				break;
			default:
				break;
			}
		}
		else if (tag instanceof FavoriteLegislatorWrapper)
			startActivity(Utils.legislatorLoadIntent(((FavoriteLegislatorWrapper) tag).legislator.id));
		else if (tag instanceof FavoriteBillWrapper) {
			Bill bill = ((FavoriteBillWrapper) tag).bill;
			startActivity(Utils.billLoadIntent(bill.id, bill.code));
		}
	}

	public void setupControls() {
		LayoutInflater inflater = LayoutInflater.from(this);
		adapter = new MergeAdapter();

		// Votes
		adapter.addView(inflateHeader(inflater, R.string.menu_votes_header));
		adapter.addAdapter(new ViewArrayAdapter(this, setupVotesMenu(inflater)));
		
		// Bills
		adapter.addView(inflateHeader(inflater, R.string.menu_bills_header));
		if (billCursor != null)
			adapter.addAdapter(new FavoriteBillsAdapter(this, billCursor));
		adapter.addAdapter(new ViewArrayAdapter(this, setupBillMenu(inflater)));
		
		// Legislators
		adapter.addView(inflateHeader(inflater, R.string.menu_legislators_header));
		if (peopleCursor != null)
			adapter.addAdapter(new FavoriteLegislatorsAdapter(this, peopleCursor));
		searchLocationAdapter = new ViewArrayAdapter(this, setupSearchMenu(inflater));
		adapter.addAdapter(searchLocationAdapter);
		adapter.addAdapter(new ViewArrayAdapter(this, setupCommitteeMenu(inflater)));
		
		setListAdapter(adapter);
		
		setupDebugBar();
	}
	
	private void setupDebugBar() {
		findViewById(R.id.check).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				WakefulIntentService.sendWakefulWork(MainMenu.this, NotificationService.class);
			}
		});
	}

	private ArrayList<View> setupBillMenu(LayoutInflater inflater) {
		ArrayList<View> billViews = new ArrayList<View>();

		billViews.add(inflateItem(inflater, R.drawable.bill_law, R.string.menu_bills_law, BILLS_LAW));
		billViews.add(inflateItem(inflater, R.drawable.bill_recent, R.string.menu_bills_recent, BILLS_RECENT));	
		billViews.add(inflateItem(inflater, R.drawable.bill_code, R.string.menu_bills_code, BILLS_CODE));

		return billViews;
	}
	
	private ArrayList<View> setupVotesMenu(LayoutInflater inflater) {
		ArrayList<View> voteViews = new ArrayList<View>();
		
		voteViews.add(inflateItem(inflater, R.drawable.rolls_menu, R.string.menu_votes_latest, VOTES_LATEST));
		voteViews.add(inflateItem(inflater, R.drawable.rolls_nominations, R.string.menu_votes_nominations, VOTES_NOMINATIONS));
		
		return voteViews;
	}

	private ArrayList<View> setupSearchMenu(LayoutInflater inflater) {
		ArrayList<View> searchViews = new ArrayList<View>(4);

		View view = inflateLocationItem(inflater, R.drawable.search_location, R.string.menu_legislators_location, SEARCH_LOCATION);
		searchLocationView = new SearchViewWrapper(view);

		View searchState = inflateItem(inflater, R.drawable.search_all, R.string.menu_legislators_state, SEARCH_STATE);
		View searchName = inflateItem(inflater, R.drawable.search_lastname, R.string.menu_legislators_lastname, SEARCH_NAME);
		View searchZip = inflateItem(inflater, R.drawable.search_zip, R.string.menu_legislators_zip, SEARCH_ZIP); 

		searchViews.add(searchLocationView.getBase());
		searchViews.add(searchState);
		searchViews.add(searchName);		
		searchViews.add(searchZip);

		return searchViews;
	}

	private ArrayList<View> setupCommitteeMenu(LayoutInflater inflater) {
		ArrayList<View> committeeViews = new ArrayList<View>(1);
		View committees = inflateItem(inflater, R.drawable.committee, R.string.menu_committees,
				SEARCH_COMMITTEE);
		committeeViews.add(committees);
		return committeeViews;
	}

	private View inflateHeader(LayoutInflater inflater, int text) {
		View view = inflater.inflate(R.layout.header_layout, null);
		((TextView) view.findViewById(R.id.header_text)).setText(text);
		return view;
	}

	private View inflateItem(LayoutInflater inflater, int icon, int text, Object tag) {
		View item = inflater.inflate(R.layout.main_menu_item, null);
		((ImageView) item.findViewById(R.id.icon)).setImageResource(icon);
		((TextView) item.findViewById(R.id.text)).setText(text);
		item.setTag(new ViewWrapper(item, tag));
		return item;
	}

	private View inflateLocationItem(LayoutInflater inflater, int icon, int text, Object tag) {
		View item = inflater.inflate(R.layout.main_menu_item_location, null);
		((ImageView) item.findViewById(R.id.icon)).setImageResource(icon);
		((TextView) item.findViewById(R.id.text_1)).setText(text);
		((TextView) item.findViewById(R.id.loading_message)).setText(R.string.menu_location_updating);
		item.setTag(new ViewWrapper(item, tag));
		return item;
	}

	private void searchByZip(String zipCode) {
		startActivity(new Intent(this, LegislatorList.class)
			.putExtra("type", LegislatorList.SEARCH_ZIP)
			.putExtra("zip_code", zipCode));
	}

	private void searchByLatLong(double latitude, double longitude, String address) {
		startActivity(new Intent(this, LegislatorList.class)
			.putExtra("type", LegislatorList.SEARCH_LOCATION)
			.putExtra("latitude", latitude)
			.putExtra("longitude", longitude)
			.putExtra("address", address));
	}

	private void searchByLastName(String lastName) {
		startActivity(new Intent(this, LegislatorList.class)
			.putExtra("type", LegislatorList.SEARCH_LASTNAME)
			.putExtra("last_name", lastName));
	}

	private void searchByState(String state) {
		startActivity(new Intent(this, LegislatorList.class)
			.putExtra("type", LegislatorList.SEARCH_STATE)
			.putExtra("state", state));
	}

	private void searchByBillId(String billId, String code) {
		startActivity(Utils.billLoadIntent(billId, code));
	}

	private void getResponse(int requestCode) {
		Intent intent = new Intent();

		switch (requestCode) {
		case RESULT_ZIP:
			intent.setClass(this, GetText.class)
			.putExtra("ask", "Enter a zip code:")
			.putExtra("hint", "e.g. 11216")
			.putExtra("startValue", Utils.getStringPreference(this, "search_zip"))
			.putExtra("inputType", InputType.TYPE_CLASS_PHONE);
			break;
		case RESULT_LASTNAME:
			intent.setClass(this, GetText.class)
			.putExtra("ask", "Enter a last name:")
			.putExtra("hint", "e.g. Schumer")
			.putExtra("startValue", Utils.getStringPreference(this, "search_lastname"))
			.putExtra("inputType", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
			break;
		case RESULT_STATE:
			intent.setClass(this, GetState.class)
			.putExtra("startValue", Utils.getStringPreference(this, "search_state"));
			break;
		case RESULT_BILL_CODE:
			intent.setClass(this, GetText.class)
			.putExtra("ask", "Enter a bill code:")
			.putExtra("hint", "e.g. \"hr4136\", \"s782\"")
			.putExtra("startValue", Utils.getStringPreference(this, "search_bill_code"))
			.putExtra("inputType", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
			break;
		default:
			break;
		}

		startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case RESULT_ZIP:
			if (resultCode == RESULT_OK) {
				String zipCode = data.getExtras().getString("response").trim();
				if (!zipCode.equals("")) {
					Utils.setStringPreference(this, "search_zip", zipCode);
					searchByZip(zipCode);
				}
			}
			break;
		case RESULT_LASTNAME:
			if (resultCode == RESULT_OK) {
				String lastName = data.getExtras().getString("response").trim();
				if (!lastName.equals("")) {
					Utils.setStringPreference(this, "search_lastname", lastName);
					searchByLastName(lastName);
				}
			}
			break;
		case RESULT_STATE:
			if (resultCode == RESULT_OK) {
				String state = data.getExtras().getString("response").trim();
				if (!state.equals("")) {
					String code = Utils.stateNameToCode(this, state);
					if (code != null) {
						Utils.setStringPreference(this, "search_state", state); // store the name, not the code
						searchByState(code);
					}
				}
			}
			break;
		case RESULT_BILL_CODE:
			if (resultCode == RESULT_OK) {
				String code = data.getExtras().getString("response").trim();
				if (!code.equals("")) {
					String billId = Bill.codeToBillId(code);
					if (billId != null) {
						Utils.setStringPreference(this, "search_bill_code", code); // store the code, not the bill_id
						searchByBillId(billId, code);
					}
				}
			}
			break;
		}
	}

	public boolean firstTime() {
		if (Utils.getBooleanPreference(this, "first_time", true)) {
			Utils.setBooleanPreference(this, "first_time", false);
			return true;
		}
		return false;
	}

	public boolean newVersion() {
		String lastVersionSeen = getVersionSeen();
		String currentVersion = getResources().getString(R.string.app_version);
		if (lastVersionSeen != null && lastVersionSeen.equals(currentVersion))
			return false;
		else {
			setVersionSeen(currentVersion);
			return true;
		}
	}

	public void setNotificationState() {
		Utils.setBooleanPreference(this, NotificationSettings.KEY_NOTIFY_ENABLED,
				NotificationSettings.DEFAULT_NOTIFY_ENABLED);
	}


	public void setVersionSeen(String version) {
		Utils.setStringPreference(this, "last_version_seen", version);
	}

	public String getVersionSeen() {
		return Utils.getStringPreference(this, "last_version_seen");
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();

		switch(id) {
		case ABOUT:
			View aboutView = inflater.inflate(R.layout.about, null);

			Spanned about1 = Html.fromHtml(
					"Bill information provided by <a href=\"http://govtrack.us\">GovTrack</a>, through the Library of Congress.  Bill summaries written by the Congressional Research Service.<br/><br/>" +
					"Legislator search and information powered by the <a href=\"http://services.sunlightlabs.com/api/\">Sunlight Labs Congress API</a>.<br/><br/>" + 
					"News mentions provided by the <a href=\"http://developer.yahoo.com/search/news/\">Yahoo! News API</a>, and Twitter search powered by <a href=\"http://www.winterwell.com/software/jtwitter.php\">JTwitter</a>."
			);
			TextView aboutView1 = (TextView) aboutView.findViewById(R.id.about_1);
			aboutView1.setText(about1);
			aboutView1.setMovementMethod(LinkMovementMethod.getInstance());

			Spanned about2 = Html.fromHtml(
					"This app is made by <a href=\"http://sunlightlabs.com\">Sunlight Labs</a>, " + 
					"a division of the <a href=\"http://sunlightfoundation.com\">Sunlight Foundation</a> " +
					"that is dedicated to increasing government transparency through the power of technology."
			);
			TextView aboutView2 = (TextView) aboutView.findViewById(R.id.about_2);
			aboutView2.setText(about2);
			aboutView2.setMovementMethod(LinkMovementMethod.getInstance());

			builder.setIcon(R.drawable.icon);
			builder.setTitle(R.string.app_name);
			builder.setView(aboutView);
			builder.setPositiveButton(R.string.about_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			return builder.create();
		case FIRST:
			View firstView = inflater.inflate(R.layout.first_time, null);

			builder.setIcon(R.drawable.icon);
			builder.setTitle(R.string.app_name);
			builder.setView(firstView);
			builder.setPositiveButton(R.string.first_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			return builder.create();
		case CHANGELOG:
			View changelogView = inflater.inflate(R.layout.changelog, null);

			Spanned changelog = Html.fromHtml(
				"<b>&#183;</b> 2.9.6 - Added a donate link<br/><br/>" +
				"<b>&#183;</b> Added <b>background notifications</b> for pretty much everything<br/><br/>" +
				"<b>&#183;</b> Fixed links to THOMAS<br/><br/>" +
				"<b>&#183;</b> Various fixes and visual tweaks"
			);
			Spanned changelogLast = Html.fromHtml(
				"<b>&#183;</b> See a legislator's recent voting record<br/><br/>" +
				"<b>&#183;</b> See recent votes in general, and just nomination votes<br/><br/>" +
				"<b>&#183;</b> Improved vote screen, linked back to bill if related<br/><br/>" +
				"<b>&#183;</b> Show cosponsors of bills<br/><br/>" +
				"<b>&#183;</b> Added legislator's office building and room number"
			);
			((TextView) changelogView.findViewById(R.id.changelog)).setText(changelog);
			((TextView) changelogView.findViewById(R.id.changelog_last_title)).setText(R.string.app_version_older);
			((TextView) changelogView.findViewById(R.id.changelog_last)).setText(changelogLast);

			builder.setIcon(R.drawable.icon);
			builder.setTitle(getResources().getString(R.string.changelog_title_prefix) + " " + getResources().getString(R.string.app_version));
			builder.setView(changelogView);
			builder.setPositiveButton(R.string.changelog_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			return builder.create();
		case DONATION:
			View donateView = inflater.inflate(R.layout.donation, null);
			
			Spanned donate2 = Html.fromHtml(
				"You can press Donate below to give through the Android Market. " +
				"If you would prefer to give more, or through the web, you can also " +
				"<a href=\"http://sunlightfoundation.com/donate/\">donate online</a>."
			);
			
			TextView donateTextView = (TextView) donateView.findViewById(R.id.donate_2);
			donateTextView.setText(donate2);
			donateTextView.setMovementMethod(LinkMovementMethod.getInstance());
			
			builder.setIcon(R.drawable.icon);
			builder.setTitle(R.string.donation_title);
			builder.setView(donateView);
			builder.setPositiveButton(R.string.donation_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			builder.setNeutralButton(R.string.donation_donate, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					donationPage();
				}
			});
			return builder.create();
		default:
			return null;
		}
	}

	@Override 
	public boolean onCreateOptionsMenu(Menu menu) { 
		super.onCreateOptionsMenu(menu); 
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) { 
		case R.id.preferences:
			startActivity(new Intent(this, NotificationSettings.class));
			break;
		case R.id.feedback:
			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getResources().getString(R.string.contact_email), null));
			intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.contact_subject));
			startActivity(intent);
			break;
		case R.id.changelog:
			showDialog(CHANGELOG);
			break;
		case R.id.about:
			showDialog(ABOUT);
			break;
		case R.id.donate:
			donationPage();
			break;
		}
		return true;
	}
	
	private void donationPage() {
		String packageName = getResources().getString(R.string.app_donation_package_name);
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
	}

	private void setupLocation() {
		if(address != null) {
			displayAddress(address, true);
			return;
		}

		location = LocationUtils.getLastKnownLocation(this);

		if (location == null) {
			toggleLocationEnabled(false);
			toggleLocationLoading(true);
			timer = LocationUtils
					.requestLocationUpdate(this, handler, LocationManager.GPS_PROVIDER);
		}
		else { 
			address = AddressUpdater.getFromCache(location); 
			if(address == null) {
				toggleLocationEnabled(false);
				toggleLocationLoading(true);
				addressUpdater = (AddressUpdater) new AddressUpdater(this).execute(location);
			}
			else
				displayAddress(address, true);
		}
	}

	private void toggleLocationEnabled(boolean enabled) {
		searchLocationView.getWrapperTag().setEnabled(enabled);
		searchLocationAdapter.notifyDataSetChanged();
		searchLocationView.getText1().setTextColor(enabled == true ? Color.parseColor("#dddddd") : Color.parseColor("#666666"));
	}

	private void toggleLocationLoading(boolean visible) {
		searchLocationView.getLoading().setVisibility(visible ? View.VISIBLE : View.GONE);
		searchLocationView.getText2().setVisibility(visible ? View.GONE : View.VISIBLE);
	}

	private void cancelTimer() {
		if (timer != null) {
			timer.cancel();
		}
	}

	private void displayAddress(String address, boolean enabled) {
		searchLocationView.getText2().setTextColor(enabled ? Color.parseColor("#dddddd") : Color.parseColor("#666666"));
		searchLocationView.getText2().setText(address);
	}

	public void onLocationUpdateError() {
		displayAddress(this.getString(R.string.menu_location_no_location), false);
		toggleLocationLoading(false);
		toggleLocationEnabled(false);
	}

	public void onLocationChanged(Location location) {
		this.location = location;
		addressUpdater = (AddressUpdater) new AddressUpdater(this).execute(location);
		cancelTimer();
	}

	public void onProviderDisabled(String provider) {}

	public void onProviderEnabled(String provider) {}

	public void onStatusChanged(String provider, int status, Bundle extras) {}

	public void onTimeout(String provider) {
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			timer = LocationUtils.requestLocationUpdate(this, handler,
					LocationManager.NETWORK_PROVIDER);
		} else
			onLocationUpdateError();
	}

	public void onAddressUpdate(String address) {
		this.address =  address;
		addressUpdater = null;
		displayAddress(address, true);	

		toggleLocationEnabled(true);
		toggleLocationLoading(false);
	}

	public void onAddressUpdateError(CongressException e) {
		this.address = "";
		addressUpdater = null;
		displayAddress(address, false);

		toggleLocationEnabled(true);
		toggleLocationLoading(false);
	}

	public void loadPhoto(String bioguide_id, FavoriteLegislatorWrapper wrapper) {
		if (!loadPhotoTasks.containsKey(bioguide_id)) {
			loadPhotoTasks.put(bioguide_id, (LoadPhotoTask) new LoadPhotoTask(this,
					LegislatorImage.PIC_MEDIUM, bioguide_id).execute(bioguide_id));
			favoritePeopleWrappers.put(bioguide_id, wrapper);
		}
	}

	public Context getContext() {
		return this;
	}

	public void onLoadPhoto(Drawable photo, Object tag) {
		String bioguide_id = (String) tag;
		loadPhotoTasks.remove(bioguide_id);
		favoritePeopleWrappers.get(bioguide_id).onLoadPhoto(photo, bioguide_id);
		favoritePeopleWrappers.remove(bioguide_id);
	}
	
	// this class is used to cache the search location view and its children
	private class SearchViewWrapper {
		private View base, loading;
		private TextView text1, text2;		

		public SearchViewWrapper(View base) {
			this.base = base;
		}		
		public TextView getText1() {
			return (text1 == null) ? text1 = (TextView) this.base.findViewById(R.id.text_1) : text1;
		}		
		public TextView getText2() {
			return (text2 == null) ? text2 = (TextView) this.base.findViewById(R.id.text_2) : text2;
		}		
		public View getLoading() {
			return (loading == null) ? loading = this.base.findViewById(R.id.row_loading) : loading;
		}		
		public View getBase() {
			return base;
		}		
		public ViewWrapper getWrapperTag() {
			return (ViewWrapper) base.getTag();
		}	
	}
	
	// Favorite bills adapter for the menu
	public class FavoriteBillsAdapter extends CursorAdapter {

		public FavoriteBillsAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			try {
				((FavoriteBillWrapper) view.getTag()).populateFrom(cursor);
			} catch(CongressException e) {
				Utils.alert(context, R.string.menu_favorite_bill_error);
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = LayoutInflater.from(context).inflate(R.layout.favorite_bill, null);
			FavoriteBillWrapper wrapper = new FavoriteBillWrapper(row);
			
			try {
				wrapper.populateFrom(cursor);
			} catch(CongressException e) {
				Utils.alert(context, R.string.menu_favorite_bill_error);
			}
			
			row.setTag(wrapper);
			return row;
		}
		
		public class FavoriteBillWrapper {
			private View row;
			private TextView code, title;
			
			public Bill bill;

			public FavoriteBillWrapper(View row) {
				this.row = row;
			}
			
			public void populateFrom(Cursor c) throws CongressException {
				bill = Database.loadBill(c);
				getCode().setText(Bill.formatCode(bill.code));
				String title;
				if (bill.short_title != null && !bill.short_title.equals(""))
					title = bill.short_title;
				else
					title = bill.official_title;
				getTitle().setText(Utils.truncate(title, 80));
			}
			
			private TextView getCode() {
				return code == null ? code = (TextView) row.findViewById(R.id.code) : code;
			}
			
			private TextView getTitle() {
				return title == null ? title = (TextView) row.findViewById(R.id.title) : title;
			}
		}
			
	}
	
	// Favorite legislators adapter for the menu
	public class FavoriteLegislatorsAdapter extends CursorAdapter {

		public FavoriteLegislatorsAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((FavoriteLegislatorWrapper) view.getTag()).populateFrom(cursor, context);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = LayoutInflater.from(context).inflate(R.layout.favorite_legislator, null);
			FavoriteLegislatorWrapper wrapper = new FavoriteLegislatorWrapper(row);
			
			wrapper.populateFrom(cursor, context);
			row.setTag(wrapper);
			
			return row;
		}

		public class FavoriteLegislatorWrapper {
			private View row;

			private TextView name, position;
			private ImageView photo;
			
			public Legislator legislator;

			public FavoriteLegislatorWrapper(View row) {
				this.row = row;
			}

			void populateFrom(Cursor c, Context context) {
				legislator = Database.loadLegislator(c);
				
				getName().setText(legislator.titledName());
				String position = Legislator.partyName(legislator.party) + " from " 
					+ Utils.stateCodeToName(context, legislator.state);
				getPosition().setText(position);
				
				BitmapDrawable picture = LegislatorImage.quickGetImage(LegislatorImage.PIC_MEDIUM,
						legislator.bioguide_id, context);
				
				if (picture != null)
					getPhoto().setImageDrawable(picture);
				else {
					getPhoto().setImageResource(R.drawable.loading_photo);

					Class<?> paramTypes[] = new Class<?>[] { String.class, FavoriteLegislatorWrapper.class };
					Object[] args = new Object[] { legislator.bioguide_id, this };
					try {
						context.getClass().getMethod("loadPhoto", paramTypes).invoke(context, args);
					} catch (Exception e) {
						Log.e(this.getClass().getName(),
								"The Context must implement LoadPhotoTask.LoadsPhoto interface!");
					}
				}
			}

			public void onLoadPhoto(Drawable photo, String bioguideId) {
				if (photo != null)
					getPhoto().setImageDrawable(photo);
				else
					getPhoto().setImageResource(R.drawable.no_photo_female);
			}
			
			private TextView getName() {
				return name == null ? name = (TextView) row.findViewById(R.id.name) : name;
			}
			
			private TextView getPosition() {
				return position == null ? position = (TextView) row.findViewById(R.id.position) : position;
			}
			
			private ImageView getPhoto() {
				return photo == null ? photo = (ImageView) row.findViewById(R.id.photo) : photo;
			}
		}
	}
}