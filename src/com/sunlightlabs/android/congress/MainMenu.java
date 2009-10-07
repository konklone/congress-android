package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainMenu extends Activity {
	private static final int MENU_PREFS = 0;
	
	public static final int RESULT_ZIP = 1;
	public static final int RESULT_LASTNAME = 2;
	public static final int RESULT_STATE = 3;
	private static final int RESULT_SHORTCUT = 10;
	private Location location;

	// whether the user has come to this activity looking to create a shortcut
	private boolean shortcut = false;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        loadLocation();
        setupControls();

        String action = getIntent().getAction();
        if (action != null && action.equals(Intent.ACTION_CREATE_SHORTCUT))
        	shortcut = true;
    }
	
	public void loadLocation() {
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		location = null;

		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}
	
	public void setupControls() {
        Button fetchZip = (Button) this.findViewById(R.id.fetch_zip);
        Button fetchLocation = (Button) this.findViewById(R.id.fetch_location);
        
    	if (location != null) {
	    	fetchLocation.setOnClickListener(new View.OnClickListener() {
	    		public void onClick(View v) {
	    			if (location != null)
	    				searchByLatLong(location.getLatitude(), location.getLongitude());
	    		}
	    	});
    	} else
    		fetchLocation.setEnabled(false);
    	
    	fetchZip.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			getResponse(RESULT_ZIP);
    		}
    	});
    	
    	Button fetchLastName = (Button) this.findViewById(R.id.fetch_last_name);
    	fetchLastName.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getResponse(RESULT_LASTNAME);
			}
		});
    	
    	Button fetchState = (Button) this.findViewById(R.id.fetch_state);
    	fetchState.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getResponse(RESULT_STATE);
			}
		});
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
		Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		
		extras.putBoolean("shortcut", shortcut);
		i.putExtras(extras);
		
		if (shortcut)
			startActivityForResult(i, RESULT_SHORTCUT);
		else
			startActivity(i);
	}
	
	private void getResponse(int requestCode) {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.GetText");
		Bundle extras = new Bundle();
		
		switch (requestCode) {
		case RESULT_ZIP:
			extras.putString("ask", "Enter a zip code:");
			extras.putString("hint", "e.g. 11216");
			extras.putInt("inputType", InputType.TYPE_CLASS_NUMBER);
			break;
		case RESULT_LASTNAME:
			extras.putString("ask", "Enter a last name:");
			extras.putString("hint", "e.g. Schumer");
			extras.putInt("inputType", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
			break;
		case RESULT_STATE:
			extras.putString("ask", "2-letter state code:");
			extras.putString("hint", "e.g. NY");
			extras.putInt("inputType", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
			break;
		default:
			break;
		}
		
		intent.putExtras(extras);
		startActivityForResult(intent, requestCode);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case RESULT_ZIP:
			if (resultCode == RESULT_OK) {
				String zipCode = data.getExtras().getString("response");
				if (!zipCode.equals(""))
					searchByZip(zipCode);
			}
			break;
		case RESULT_LASTNAME:
			if (resultCode == RESULT_OK) {
				String lastName = data.getExtras().getString("response");
				if (!lastName.equals(""))
					searchByLastName(lastName);
			}
			break;
		case RESULT_STATE:
			if (resultCode == RESULT_OK) {
				String state = data.getExtras().getString("response");
				if (!state.equals(""))
					searchByState(state);
			}
			break;
		case RESULT_SHORTCUT:
			if (resultCode == RESULT_OK) {
				setResult(RESULT_OK, data);
	    		finish();
			}
			break;
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
    	case R.id.feedback:
    		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getResources().getString(R.string.contact_email), null));
    		intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.contact_subject));
    		startActivity(intent);
    	}
    	return true;
    }
}