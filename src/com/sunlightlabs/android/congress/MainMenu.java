package com.sunlightlabs.android.congress;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainMenu extends Activity {
	public static final int RESULT_ZIP = 1;
	public static final int RESULT_LASTNAME = 2;
	private static final int RESULT_SHORTCUT = 3;
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
    			Intent intent = new Intent();
    			intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.GetText");
    			Bundle extras = new Bundle();
    			extras.putString("ask", "Enter a zip code:");
    			extras.putString("hint", "e.g. 11216");
    			intent.putExtras(extras);
    			startActivityForResult(intent, RESULT_ZIP);
    		}
    	});
    	
    	Button fetchLastName = (Button) this.findViewById(R.id.fetch_last_name);
    	fetchLastName.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchByLastName("kennedy");
			}
		});
    }
	
	public void searchByZip(String zipCode) {
    	Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		
		Bundle extras = new Bundle();
		extras.putString("zip_code", zipCode);
		extras.putBoolean("shortcut", shortcut);
		i.putExtras(extras);
		
		if (shortcut)
			startActivityForResult(i, RESULT_SHORTCUT);
		else
			startActivity(i);
    }
	
	public void searchByLatLong(double latitude, double longitude) {
		Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		
		Bundle extras = new Bundle();
		extras.putDouble("latitude", latitude);
		extras.putDouble("longitude", longitude);
		extras.putBoolean("shortcut", shortcut);
		i.putExtras(extras);
		
		if (shortcut)
			startActivityForResult(i, RESULT_SHORTCUT);
		else
			startActivity(i);
	}
	
	public void searchByLastName(String lastName) {
		Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		
		Bundle extras = new Bundle();
		extras.putString("last_name", lastName);
		extras.putBoolean("shortcut", shortcut);
		i.putExtras(extras);
		
		if (shortcut)
			startActivityForResult(i, RESULT_SHORTCUT);
		else
			startActivity(i);
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
		case RESULT_SHORTCUT:
			if (resultCode == RESULT_OK) {
				setResult(RESULT_OK, data);
	    		finish();
			}
			break;
		}
	}
}