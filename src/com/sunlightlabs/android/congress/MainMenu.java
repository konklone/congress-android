package com.sunlightlabs.android.congress;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;

public class MainMenu extends ListActivity {
	public static final int RESULT_ZIP = 1;
	public static final int RESULT_LASTNAME = 2;
	public static final int RESULT_STATE = 3;
	
	private static final int ABOUT = 0;
	private static final int FIRST = 1;
	private static final int CHANGELOG = 2;
	
	public static final int SEARCH_LOCATION = 2;
	public static final int SEARCH_ZIP = 3;
	public static final int SEARCH_STATE = 4;
	public static final int SEARCH_NAME = 5;
	
	private Location location;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        
        location = getLocation();
        setupControls();
        
        if (firstTime()) {
        	newVersion(); // don't need to see the changelog on first install
        	showDialog(FIRST);
        } else if (newVersion())
        	showDialog(CHANGELOG);
    }
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		int type = ((Integer) v.getTag()).intValue();
		switch(type) {
		case SEARCH_LOCATION:
			searchByLatLong(location.getLatitude(), location.getLongitude());
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
		case BillList.BILLS_RECENT:
		case BillList.BILLS_LAW:
			startActivity(new Intent(this, BillList.class).putExtra("type", type));
		default:
			break;
		}
    }
	
	public void setupControls() {
        LayoutInflater inflater = LayoutInflater.from(this);
        
        LinearLayout billsHeader = (LinearLayout) inflater.inflate(R.layout.header_layout, null);
        ((TextView) billsHeader.findViewById(R.id.header_text)).setText(R.string.menu_bills_header);
        
        ArrayList<View> billViews = new ArrayList<View>(2);
        
        LinearLayout billsLaw = (LinearLayout) inflater.inflate(R.layout.main_menu_item, null);
        ((ImageView) billsLaw.findViewById(R.id.icon)).setImageResource(R.drawable.bill_law);
        ((TextView) billsLaw.findViewById(R.id.text)).setText(R.string.menu_bills_law);
        billsLaw.setTag(BillList.BILLS_LAW);
        billViews.add(billsLaw);
        
        LinearLayout billsRecent = (LinearLayout) inflater.inflate(R.layout.main_menu_item, null);
        ((ImageView) billsRecent.findViewById(R.id.icon)).setImageResource(R.drawable.bill_recent);
        ((TextView) billsRecent.findViewById(R.id.text)).setText(R.string.menu_bills_recent);
        billsRecent.setTag(BillList.BILLS_RECENT);
        billViews.add(billsRecent);
        
        
        LinearLayout peopleHeader = (LinearLayout) inflater.inflate(R.layout.header_layout, null);
        ((TextView) peopleHeader.findViewById(R.id.header_text)).setText(R.string.menu_legislators_header);
        peopleHeader.setEnabled(false);
        
        ArrayList<View> searchViews = new ArrayList<View>(4);
        
        if (location != null) {
	        LinearLayout searchLocation = (LinearLayout) inflater.inflate(R.layout.main_menu_item, null);
	        ((ImageView) searchLocation.findViewById(R.id.icon)).setImageResource(R.drawable.search_location);
	        ((TextView) searchLocation.findViewById(R.id.text)).setText(R.string.menu_legislators_location);
	        searchLocation.setTag(SEARCH_LOCATION);
	        searchViews.add(searchLocation);
        }
        
        LinearLayout searchState = (LinearLayout) inflater.inflate(R.layout.main_menu_item, null);
        ((ImageView) searchState.findViewById(R.id.icon)).setImageResource(R.drawable.search_all);
        ((TextView) searchState.findViewById(R.id.text)).setText(R.string.menu_legislators_state);
        searchState.setTag(SEARCH_STATE);
        searchViews.add(searchState);
        
        LinearLayout searchName = (LinearLayout) inflater.inflate(R.layout.main_menu_item, null);
        ((ImageView) searchName.findViewById(R.id.icon)).setImageResource(R.drawable.search_lastname);
        ((TextView) searchName.findViewById(R.id.text)).setText(R.string.menu_legislators_lastname);
        searchName.setTag(SEARCH_NAME);
        searchViews.add(searchName);
        
        LinearLayout searchZip = (LinearLayout) inflater.inflate(R.layout.main_menu_item, null);
        ((ImageView) searchZip.findViewById(R.id.icon)).setImageResource(R.drawable.search_zip);
        ((TextView) searchZip.findViewById(R.id.text)).setText(R.string.menu_legislators_zip);
        searchZip.setTag(SEARCH_ZIP);
        searchViews.add(searchZip);
        
        MergeAdapter adapter = new MergeAdapter();
        adapter.addView(billsHeader);
        adapter.addAdapter(new ViewArrayAdapter(this, billViews));
        
        adapter.addView(peopleHeader);
        if (location == null) {   
	        LinearLayout searchLocation = (LinearLayout) inflater.inflate(R.layout.icon_list_item_1_disabled, null);
	        ((ImageView) searchLocation.findViewById(R.id.icon)).setImageResource(R.drawable.search_location);
	        ((TextView) searchLocation.findViewById(R.id.text)).setText(R.string.menu_legislators_location);
	        adapter.addView(searchLocation);
        }
        adapter.addAdapter(new ViewArrayAdapter(this, searchViews));
        
        setListAdapter(adapter);
    }
	
	public Location getLocation() {
		Location location = null;
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		if (location == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		
		return location;
	}
	
	public void searchByZip(String zipCode) {
		Bundle extras = new Bundle();
		extras.putString("zip_code", zipCode);
		search(extras);
    }
	
	public void searchByLatLong(double latitude, double longitude) {
		Bundle extras = new Bundle();
		extras.putDouble("latitude", latitude);
		extras.putDouble("longitude", longitude);
		search(extras);
	}
	
	public void searchByLastName(String lastName) {
		Bundle extras = new Bundle();
		extras.putString("last_name", lastName);
		search(extras);
	}
	
	public void searchByState(String state) {
		Bundle extras = new Bundle();
		extras.putString("state", state);
		search(extras);
	}
	
	private void search(Bundle extras) {
		startActivity(new Intent(this, LegislatorList.class).putExtras(extras));
	}
	
	private void getResponse(int requestCode) {
		Intent intent = new Intent();
		
		
		switch (requestCode) {
		case RESULT_ZIP:
			intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.GetText");
			intent.putExtra("ask", "Enter a zip code:");
			intent.putExtra("hint", "e.g. 11216");
			intent.putExtra("startValue", Preferences.getString(this, "search_zip"));
			intent.putExtra("inputType", InputType.TYPE_CLASS_PHONE);
			break;
		case RESULT_LASTNAME:
			intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.GetText");
			intent.putExtra("ask", "Enter a last name:");
			intent.putExtra("hint", "e.g. Schumer");
			intent.putExtra("startValue", Preferences.getString(this, "search_lastname"));
			intent.putExtra("inputType", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
			break;
		case RESULT_STATE:
			intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.GetState");
			intent.putExtra("startValue", Preferences.getString(this, "search_state"));
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
					String code = Utils.stateNameToCode(this, state.trim());
					if (code != null) {
						Preferences.setString(this, "search_state", state); // store the name, not the code
						searchByState(code);
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
        		"<b>&#183;</b> Basic information about recently introduced bills, and laws.<br/><br/>" +
        		"<b>&#183;</b> New \"Sponsored Bills\" button on legislator profiles.<br/><br/>" +
        		"<b>&#183;</b> Tap the menu button while viewing a legislator or bill to create a home screen shortcut."
        	);
        	((TextView) changelogView.findViewById(R.id.changelog)).setText(changelog);
        	
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
}