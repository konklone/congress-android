package com.sunlightlabs.android.congress;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.AddressUpdater;
import com.sunlightlabs.android.congress.utils.LocationUpdater;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.android.congress.utils.ViewWrapper;
import com.sunlightlabs.android.congress.utils.AddressUpdater.AddressUpdateable;
import com.sunlightlabs.android.congress.utils.LocationUpdater.LocationUpdateable;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;

public class MainMenu extends ListActivity implements LocationUpdateable<MainMenu>, AddressUpdateable<MainMenu> {
	public static final int RESULT_ZIP = 1;
	public static final int RESULT_LASTNAME = 2;
	public static final int RESULT_STATE = 3;
	public static final int RESULT_BILL_CODE = 4;

	private static final int ABOUT = 0;
	private static final int FIRST = 1;
	private static final int CHANGELOG = 2;

	public static final int BILLS_LAW = 0;
	public static final int BILLS_RECENT = 1;
	public static final int BILLS_LATEST_VOTES = 2;
	public static final int BILLS_CODE = 3;
	public static final int SEARCH_LOCATION = 4;
	public static final int SEARCH_ZIP = 5;
	public static final int SEARCH_STATE = 6;
	public static final int SEARCH_NAME = 7;

	private Location location;
	private String address;

	private LocationUpdater locationUpdater;
	private AddressUpdater addressUpdater;

	private SearchViewWrapper searchLocationView;
	private ViewArrayAdapter searchLocationAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);

		MainMenuHolder holder = (MainMenuHolder) getLastNonConfigurationInstance();
		if(holder != null) {
			locationUpdater = holder.locationUpdater;
			addressUpdater = holder.addressUpdater;
			location = holder.location;
			address = holder.address;
		}

		if(locationUpdater == null)
			locationUpdater = new LocationUpdater(this);
		else
			locationUpdater.onScreenLoad(this);

		if(addressUpdater != null)
			addressUpdater.onScreenLoad(this);

		setupControls();

		if (firstTime()) {
			newVersion(); // don't need to see the changelog on first install
			showDialog(FIRST);
		} else if (newVersion())
			showDialog(CHANGELOG);
	}

	static class MainMenuHolder {
		AddressUpdater addressUpdater;
		LocationUpdater locationUpdater;
		Location location;
		String address;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		MainMenuHolder holder = new MainMenuHolder();
		holder.addressUpdater = addressUpdater;
		holder.locationUpdater = locationUpdater;
		holder.location = location;
		holder.address = address;
		return holder;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ViewWrapper wrapper = (ViewWrapper) v.getTag();		
		int type = ((Integer) wrapper.getTag()).intValue();
		switch(type) {
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
		case BILLS_RECENT:
			startActivity(new Intent(this, BillList.class).putExtra("type", BillList.BILLS_RECENT));
			break;
		case BILLS_LAW:
			startActivity(new Intent(this, BillList.class).putExtra("type", BillList.BILLS_LAW));
			break;
		case BILLS_LATEST_VOTES:
			startActivity(new Intent(this, BillList.class).putExtra("type", BillList.BILLS_LATEST_VOTES));
			break;
		case BILLS_CODE:
			getResponse(RESULT_BILL_CODE);
			break;
		default:
			break;
		}
	}

	// this class is used to cache the search location view and its children
	private class SearchViewWrapper {
		private View base;
		private View loading;
		private View content;
		private TextView text1;
		private TextView text2;		

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
		public View getContent() {
			return (content == null) ? content = this.base.findViewById(R.id.row_content) : content;
		}
		public View getBase() {
			return base;
		}
		public ViewWrapper getWrapperTag() {
			return (ViewWrapper) base.getTag();
		}	
	}

	public void setupControls() {
		LayoutInflater inflater = LayoutInflater.from(this);

		View billsHeader = inflateHeader(inflater, R.string.menu_bills_header);
		View peopleHeader = inflateHeader(inflater, R.string.menu_legislators_header);
		peopleHeader.setEnabled(false);

		MergeAdapter adapter = new MergeAdapter();
		adapter.addView(billsHeader);
		adapter.addAdapter(new ViewArrayAdapter(this, setupBillMenu(inflater)));

		adapter.addView(peopleHeader);		
		searchLocationAdapter = new ViewArrayAdapter(this, setupSearchMenu(inflater));
		adapter.addAdapter(searchLocationAdapter);

		setListAdapter(adapter);

		updateCurrentLocation();		
	}

	private ArrayList<View> setupBillMenu(LayoutInflater inflater) {
		ArrayList<View> billViews = new ArrayList<View>(2);

		View billsLaw = inflateItem(inflater, R.drawable.bill_law, R.string.menu_bills_law, BILLS_LAW);
		View billsRecent = inflateItem(inflater, R.drawable.bill_recent, R.string.menu_bills_recent, BILLS_RECENT);
		View billsLatestVotes = inflateItem(inflater, R.drawable.bill_vote, R.string.menu_bills_latest_votes, BILLS_LATEST_VOTES);
		View billsCode = inflateItem(inflater, R.drawable.bill_code, R.string.menu_bills_code, BILLS_CODE);

		billViews.add(billsLaw);
		billViews.add(billsRecent);	
		billViews.add(billsLatestVotes);
		billViews.add(billsCode);

		return billViews;
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
		View item = inflater.inflate(R.layout.icon_list_item_3, null);
		((ImageView) item.findViewById(R.id.icon)).setImageResource(icon);
		((TextView) item.findViewById(R.id.text_1)).setText(text);
		((TextView) item.findViewById(R.id.loading_message)).setText(R.string.menu_location_updating);
		item.setTag(new ViewWrapper(item, tag));
		return item;
	}

	private void searchByZip(String zipCode) {
		Bundle extras = new Bundle();
		extras.putString("zip_code", zipCode);
		search(extras);
	}

	private void searchByLatLong(double latitude, double longitude, String address) {
		Bundle extras = new Bundle();
		extras.putDouble("latitude", latitude);
		extras.putDouble("longitude", longitude);
		extras.putString("address", address);
		search(extras);
	}

	private void searchByLastName(String lastName) {
		Bundle extras = new Bundle();
		extras.putString("last_name", lastName);
		search(extras);
	}

	private void searchByState(String state) {
		Bundle extras = new Bundle();
		extras.putString("state", state);
		search(extras);
	}

	private void search(Bundle extras) {
		startActivity(new Intent(this, LegislatorList.class).putExtras(extras));
	}

	private void searchByBillId(String billId, String code) {
		startActivity(Utils.billIntent(billId, code));
	}

	private void getResponse(int requestCode) {
		Intent intent = new Intent();

		switch (requestCode) {
		case RESULT_ZIP:
			intent.setClass(this, GetText.class)
			.putExtra("ask", "Enter a zip code:")
			.putExtra("hint", "e.g. 11216")
			.putExtra("startValue", Preferences.getString(this, "search_zip"))
			.putExtra("inputType", InputType.TYPE_CLASS_PHONE);
			break;
		case RESULT_LASTNAME:
			intent.setClass(this, GetText.class)
			.putExtra("ask", "Enter a last name:")
			.putExtra("hint", "e.g. Schumer")
			.putExtra("startValue", Preferences.getString(this, "search_lastname"))
			.putExtra("inputType", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
			break;
		case RESULT_STATE:
			intent.setClass(this, GetState.class)
			.putExtra("startValue", Preferences.getString(this, "search_state"));
			break;
		case RESULT_BILL_CODE:
			intent.setClass(this, GetText.class)
			.putExtra("ask", "Enter a bill code:")
			.putExtra("hint", "e.g. \"HR 4136\", \"s 782\"")
			.putExtra("startValue", Preferences.getString(this, "search_bill_code"))
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
					Preferences.setString(this, "search_zip", zipCode);
					searchByZip(zipCode);
				}
			}
			break;
		case RESULT_LASTNAME:
			if (resultCode == RESULT_OK) {
				String lastName = data.getExtras().getString("response").trim();
				if (!lastName.equals("")) {
					Preferences.setString(this, "search_lastname", lastName);
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
						Preferences.setString(this, "search_state", state); // store the name, not the code
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
						Preferences.setString(this, "search_bill_code", code); // store the code, not the bill_id
						searchByBillId(billId, code);
					}
				}
			}
			break;
		}
	}

	public boolean firstTime() {
		if (Preferences.getBoolean(this, "first_time", true)) {
			Preferences.setBoolean(this, "first_time", false);
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


	public void setVersionSeen(String version) {
		Preferences.setString(this, "last_version_seen", version);
	}

	public String getVersionSeen() {
		return Preferences.getString(this, "last_version_seen");
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();

		switch(id) {
		case ABOUT:
			ScrollView aboutView = (ScrollView) inflater.inflate(R.layout.about, null);

			Spanned about1 = Html.fromHtml(
					"Bill information provided by <a href=\"http://govtrack.us\">GovTrack</a>, through the Library of Congress.  Bill summaries written by the Congressional Research Service.<br/><br/>" +
					"Legislator search and information powered by the <a href=\"http://services.sunlightlabs.com/api/\">Sunlight Labs API</a>.<br/><br/>" + 
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
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			return builder.create();
		case FIRST:
			ScrollView firstView = (ScrollView) inflater.inflate(R.layout.first_time, null);

			builder.setIcon(R.drawable.icon);
			builder.setTitle(R.string.app_name);
			builder.setView(firstView);
			builder.setPositiveButton(R.string.first_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			return builder.create();
		case CHANGELOG:
			ScrollView changelogView = (ScrollView) inflater.inflate(R.layout.changelog, null);

			Spanned changelog = Html.fromHtml(
					"<b>&#183;</b> New \"Latest Votes\" listing for bills and resolutions that just got a vote<br/><br/>" +
					"<b>&#183;</b> Menu links to THOMAS, OpenCongress, GovTrack, and the Bioguide<br/><br/>" +
					"<b>&#183;</b> Slightly better timeline for bills<br/><br/>" +
					"<b>&#183;</b> Endless scrolling for bills, thumbnails for legislators"
			);
			Spanned changelogLast = Html.fromHtml(
					"<b>&#183;</b> Basic information about bills and laws<br/><br/>" +
					"<b>&#183;</b> New \"Sponsored Bills\" button on legislator profiles<br/><br/>" +
					"<b>&#183;</b> Made everything look nicer"
			);
			((TextView) changelogView.findViewById(R.id.changelog)).setText(changelog);
			((TextView) changelogView.findViewById(R.id.changelog_last_title)).setText("Version 2.0 - 2.1");
			((TextView) changelogView.findViewById(R.id.changelog_last)).setText(changelogLast);

			builder.setIcon(R.drawable.icon);
			builder.setTitle(getResources().getString(R.string.changelog_title_prefix) + " " + getResources().getString(R.string.app_version));
			builder.setView(changelogView);
			builder.setPositiveButton(R.string.changelog_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
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
		case R.id.settings: 
			startActivity(new Intent(this, Preferences.class));
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
		}
		return true;
	}

	private void updateCurrentLocation() {
		if(address != null) {
			displayAddress(address);
			return;
		}

		location = locationUpdater.getLastKnownLocation();

		if(location == null) {
			toggleLocationEnabled(false);
			toggleLocationLoading(true);
			locationUpdater.requestLocationUpdate();
		}
		else { 
			address = AddressUpdater.getFromCache(location); 
			if(address == null) {
				toggleLocationEnabled(false);
				toggleLocationLoading(true);
				addressUpdater = (AddressUpdater) new AddressUpdater(this).execute(location);
			}
			else
				displayAddress(address);
		}
	}

	private void toggleLocationEnabled(boolean enabled) {
		searchLocationView.getWrapperTag().setEnabled(enabled);
		searchLocationAdapter.notifyDataSetChanged();
		searchLocationView.getText1().setTextColor(enabled == true ? Color.parseColor("#dddddd") : Color.parseColor("#666666"));
	}

	private void toggleLocationLoading(boolean visible) {
		searchLocationView.getLoading().setVisibility(visible ? View.VISIBLE : View.GONE);
		searchLocationView.getContent().setVisibility(visible ? View.GONE : View.VISIBLE);
	}

	private void displayAddress(String address) {
		searchLocationView.getText2().setText(address);
	}

	public void onLocationUpdate(Location location) {
		this.location = location;
		addressUpdater = (AddressUpdater) new AddressUpdater(this).execute(location);
	}

	public void onLocationUpdateError(CongressException e) {
		displayAddress(this.getString(R.string.menu_location_no_location));

		toggleLocationLoading(false);
		toggleLocationEnabled(false);		
	}

	public void onAddressUpdate(String address) {
		this.address =  address;
		displayAddress(address);	

		addressUpdater = null;

		toggleLocationEnabled(true);
		toggleLocationLoading(false);		
	}

	public void onAddressUpdateError(CongressException e) {
		this.address = "";
		displayAddress(address);

		addressUpdater = null;

		toggleLocationEnabled(true);
		toggleLocationLoading(false);	
	}
}